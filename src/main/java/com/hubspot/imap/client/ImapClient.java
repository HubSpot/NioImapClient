package com.hubspot.imap.client;

import com.google.seventeen.common.base.Throwables;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.protocol.command.BaseCommand;
import com.hubspot.imap.protocol.command.Command;
import com.hubspot.imap.protocol.command.CommandType;
import com.hubspot.imap.protocol.command.ListCommand;
import com.hubspot.imap.protocol.command.OpenCommand;
import com.hubspot.imap.protocol.command.XOAuth2Command;
import com.hubspot.imap.protocol.command.fetch.FetchCommand;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;
import com.hubspot.imap.protocol.exceptions.AuthenticationFailedException;
import com.hubspot.imap.protocol.exceptions.ConnectionClosedException;
import com.hubspot.imap.protocol.response.ContinuationResponse;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.events.ByeEvent;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.ListResponse;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImapClient extends ChannelDuplexHandler implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapClient.class);

  private final ImapConfiguration configuration;
  private final Bootstrap bootstrap;
  private final EventExecutorGroup promiseExecutor;
  private final EventExecutorGroup idleExecutor;
  private final String userName;
  private final String authToken;
  private final ImapClientState clientState;
  private final ImapCodec codec;
  private final ConcurrentLinkedQueue<PendingCommand> pendingWriteQueue;
  private final AtomicBoolean connectionClosed;

  private Channel channel;

  private final Promise<Void> loginPromise;

  private volatile Promise currentCommandPromise;

  public ImapClient(ImapConfiguration configuration, Bootstrap bootstrap, EventExecutorGroup promiseExecutor, EventExecutorGroup idleExecutor, String userName, String authToken) {
    this.configuration = configuration;
    this.bootstrap = bootstrap;
    this.promiseExecutor = promiseExecutor;
    this.idleExecutor = idleExecutor;
    this.userName = userName;
    this.authToken = authToken;
    this.clientState = new ImapClientState(promiseExecutor);
    this.codec = new ImapCodec(clientState);
    this.pendingWriteQueue = new ConcurrentLinkedQueue<>();
    this.connectionClosed = new AtomicBoolean(false);

    loginPromise = promiseExecutor.next().newPromise();
  }

  public ChannelFuture connect() {
    ChannelFuture future = bootstrap.connect(configuration.getHostAndPort().getHostText(),
        configuration.getHostAndPort().getPort());

    future.addListener(f -> {
      if (f.isSuccess()) {
        configureChannel(((ChannelFuture) f).channel());
      }
    });

    if (pendingWriteQueue.peek() != null) {
      try {
        writeNext();
      } catch (ConnectionClosedException e) {
        LOGGER.debug("Connection closed during reconnect, this shouldnt happen", e);
      }
    }

    return future;
  }

  private void configureChannel(Channel channel) {
    this.channel = channel;
    this.channel.pipeline().addLast(codec);
    this.channel.pipeline().addLast(this);
    this.channel.pipeline().addLast(promiseExecutor, this.clientState);
  }

  public ImapClientState getState() {
    return clientState;
  }

  public Future<TaggedResponse> login() throws ConnectionClosedException {
    Future<TaggedResponse> loginFuture;
    switch (configuration.getAuthType()) {
      case XOAUTH2:
        loginFuture = oauthLogin();
        break;
      default:
        loginFuture = passwordLogin();
        break;
    }

    loginFuture.addListener(future -> {
      Object response = future.get();
      if (response instanceof ContinuationResponse) {
        loginPromise.setFailure(AuthenticationFailedException.fromContinuation(((ContinuationResponse) response).getMessage()));
      } else {
        TaggedResponse taggedResponse = ((TaggedResponse) response);
        if (taggedResponse.getCode() == ResponseCode.BAD) {
          loginPromise.setFailure(new AuthenticationFailedException(taggedResponse.getMessage()));
        } else {
          loginPromise.setSuccess(null);
        }
      }
    });

    loginPromise.addListener(future -> {
      if (future.isSuccess()) {
        startKeepAlive();
      }
    });

    return loginFuture;
  }

  private void startKeepAlive() {
    int keepAliveInterval = configuration.getNoopKeepAliveIntervalSec();
    if (keepAliveInterval > 0) {
      this.channel.pipeline().addFirst(new IdleStateHandler(keepAliveInterval, keepAliveInterval, keepAliveInterval));
    }
  }

  private Future<TaggedResponse> passwordLogin() throws ConnectionClosedException {
    return send(new BaseCommand(CommandType.LOGIN, userName, authToken));
  }

  private Future<TaggedResponse> oauthLogin() throws ConnectionClosedException {
    return send(new XOAuth2Command(userName, authToken));
  }

  public Future<TaggedResponse> logout() throws ConnectionClosedException {
    return send(new BaseCommand(CommandType.LOGOUT));
  }

  public Future<ListResponse> list(String context, String query) throws ConnectionClosedException {
    return send(new ListCommand(context, query));
  }

  public Future<OpenResponse> open(String folderName, boolean readOnly) throws ConnectionClosedException {
    return send(new OpenCommand(folderName, readOnly));
  }

  public Future<FetchResponse> fetch(long startId, Optional<Long> stopId, FetchDataItem... fetchDataItems) throws ConnectionClosedException {
    return send(new FetchCommand(startId, stopId, fetchDataItems));
  }

  public Future<NoopResponse> noop() throws ConnectionClosedException {
    return send(CommandType.NOOP);
  }

  public boolean isLoggedIn() {
    return loginPromise.isSuccess() && channel.isActive();
  }

  public boolean isConnected() {
    return channel != null && channel.isActive();
  }

  public void awaitLogin() throws InterruptedException, ExecutionException {
    loginPromise.get();
  }

  public <T extends TaggedResponse> Future<T> send(CommandType commandType, String... args) throws ConnectionClosedException {
    BaseCommand baseCommand = new BaseCommand(commandType, args);
    return send(baseCommand);
  }

  /**
   * Sends a command.
   *
   * This method will wait for any currently running command to finish before executing, this is a blocking operation.
   *
   * NOTE: It is critical that this method never be called from the event loop. This should not be a problem for user level application, but internal operations like keep-alive must be very careful to use separate executors when calling this method.
   *       The other important thing that one must pay attention too is that the threads used to execute this method not be from the same group as used to generate the promises, because the threads are given out at random and calling promise.await on the thread that is responsible for that promise will deadlock.
   * @param command Command to send
   * @param <T> Response type
   * @return Response future. Will be completed when a tagged response is received for this command.
   */
  public synchronized <T extends TaggedResponse> Future<T> send(Command command) throws ConnectionClosedException {
    final Promise<T> commandPromise = promiseExecutor.next().newPromise();
    commandPromise.addListener((f) -> {
      writeNext();
    });

    send(command, commandPromise);

    return commandPromise;
  }

  public synchronized void send(Command command, Promise promise) throws ConnectionClosedException {
    if (connectionClosed.get()) {
      throw new ConnectionClosedException("Cannot write to closed connection.");
    }

    if ((currentCommandPromise != null && !currentCommandPromise.isDone()) || !isConnected()) {
      PendingCommand pendingCommand = PendingCommand.newInstance(command, promise);
      pendingWriteQueue.add(pendingCommand);
    } else {
      actuallySend(command, promise);
    }
  }

  public void actuallySend(Command command, Promise promise) {
    currentCommandPromise = promise;

    clientState.setCurrentCommand(command);
    channel.writeAndFlush(command);
  }

  public synchronized void writeNext() throws ConnectionClosedException {
    PendingCommand pendingCommand = pendingWriteQueue.poll();
    if (pendingCommand != null) {
      send(pendingCommand.command, pendingCommand.promise);
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (configuration.getReconnectBackoffMs() >= 0 && !connectionClosed.get()) {
      ctx.executor().schedule(() -> this.connect(), configuration.getReconnectBackoffMs(), TimeUnit.MILLISECONDS);
    }
    super.channelInactive(ctx);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ContinuationResponse) {
      currentCommandPromise.setSuccess(msg);
    } else if (msg instanceof TaggedResponse) {
      TaggedResponse taggedResponse = ((TaggedResponse) msg);

      if (currentCommandPromise.isDone() && !currentCommandPromise.isSuccess()) {
        LOGGER.debug("Got tagged response to failed command, skipping");
        return;
      }
      try {
        currentCommandPromise.setSuccess(taggedResponse);
      } catch (IllegalStateException e) {
        LOGGER.debug("Could not complete current command", e);
      }
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      idleExecutor.next().submit(() -> {
        noop();
        return null;
      });
    } else if (evt instanceof ByeEvent) {
      if (channel.isOpen() && clientState.getCurrentCommand().getCommandType() != CommandType.LOGOUT) {
        channel.close();
      }
    }

    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.error("Error in handler", cause);
    if (currentCommandPromise != null) {
      currentCommandPromise.setFailure(cause);
    } else {
      ctx.fireExceptionCaught(cause);
    }
  }

  @Override
  public void close() {
    if (isConnected()) {
      if (currentCommandPromise != null && !currentCommandPromise.isDone()) {
        try {
          if (!currentCommandPromise.await(10, TimeUnit.SECONDS)) {
            connectionClosed.set(true);

            pendingWriteQueue.iterator().forEachRemaining(c -> c.promise.setFailure(new ConnectionClosedException()));
            currentCommandPromise.cancel(true);
          }
        } catch (InterruptedException e) {
          throw Throwables.propagate(e);
        }
      }

      Promise<TaggedResponse> logoutPromise = promiseExecutor.next().newPromise();
      actuallySend(new BaseCommand(CommandType.LOGOUT), logoutPromise);
      try {
        logoutPromise.get(10, TimeUnit.SECONDS);
      } catch (InterruptedException|ExecutionException|TimeoutException e) {
        LOGGER.error("Caught exception while closing client", e);
      } finally {
        channel.close();
      }
    }
  }

  private static final class PendingCommand {
    private static final Recycler<PendingCommand> RECYCLER = new Recycler<PendingCommand>() {
      @Override
      protected PendingCommand newObject(Handle handle) {
        return new PendingCommand(handle);
      }
    };

    private final Recycler.Handle handle;

    private Command command;
    private Promise promise;

    public PendingCommand(Handle handle) {
      this.handle = handle;
    }

    static PendingCommand newInstance(Command command, Promise promise) {
      PendingCommand pendingCommand = RECYCLER.get();

      pendingCommand.command = command;
      pendingCommand.promise = promise;

      return pendingCommand;
    }

    private void recycle() {
      command = null;
      promise = null;
      RECYCLER.recycle(this, handle);
    }
  }
}
