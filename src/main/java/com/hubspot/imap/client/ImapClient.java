package com.hubspot.imap.client;

import com.google.common.base.Preconditions;
import com.google.seventeen.common.base.Throwables;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.protocol.ResponseDecoder;
import com.hubspot.imap.protocol.command.BaseImapCommand;
import com.hubspot.imap.protocol.command.ImapCommand;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.ListCommand;
import com.hubspot.imap.protocol.command.OpenCommand;
import com.hubspot.imap.protocol.command.SilentStoreCommand;
import com.hubspot.imap.protocol.command.StoreCommand.StoreAction;
import com.hubspot.imap.protocol.command.XOAuth2Command;
import com.hubspot.imap.protocol.command.fetch.FetchCommand;
import com.hubspot.imap.protocol.command.fetch.StreamingFetchCommand;
import com.hubspot.imap.protocol.command.fetch.UidCommand;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;
import com.hubspot.imap.protocol.command.search.SearchCommand;
import com.hubspot.imap.protocol.command.search.keys.SearchKey;
import com.hubspot.imap.protocol.exceptions.AuthenticationFailedException;
import com.hubspot.imap.protocol.exceptions.ConnectionClosedException;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.MessageFlag;
import com.hubspot.imap.protocol.response.ContinuationResponse;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.events.ByeEvent;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.ListResponse;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.protocol.response.tagged.SearchResponse;
import com.hubspot.imap.protocol.response.tagged.StreamingFetchResponse;
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
import java.io.Closeable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ImapClient extends ChannelDuplexHandler implements AutoCloseable, Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapClient.class);

  private static final String KEEP_ALIVE_HANDLER = "imap noop keep alive";

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

  private final Promise<TaggedResponse> loginPromise;

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

  public synchronized ChannelFuture connect() {
    ChannelFuture future = bootstrap.connect(configuration.getHostAndPort().getHostText(),
        configuration.getHostAndPort().getPort());

    future.addListener(f -> {
      if (f.isSuccess()) {
        configureChannel(((ChannelFuture) f).channel());

        if (pendingWriteQueue.peek() != null) {
          idleExecutor.submit(() -> {
            writeNext();
            return null;
          });
        }
      }
    });

    return future;
  }

  private void configureChannel(Channel channel) {
    this.channel = channel;
    this.channel.pipeline().addLast(new ResponseDecoder(configuration, clientState, promiseExecutor));
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
          loginPromise.setSuccess(taggedResponse);
        }
      }
    });

    loginPromise.addListener(future -> {
      if (future.isSuccess()) {
        startKeepAlive();
      }
    });

    return loginPromise;
  }

  private void startKeepAlive() {
    int keepAliveInterval = configuration.getNoopKeepAliveIntervalSec();
    if (keepAliveInterval > 0) {
      if (!connectionClosed.get() && channel.pipeline().get(KEEP_ALIVE_HANDLER) == null) {
        this.channel.pipeline().addFirst(KEEP_ALIVE_HANDLER, new IdleStateHandler(keepAliveInterval, keepAliveInterval, keepAliveInterval));
      }
    }
  }

  private Future<TaggedResponse> passwordLogin() throws ConnectionClosedException {
    return send(new BaseImapCommand(ImapCommandType.LOGIN, userName, authToken));
  }

  private Future<TaggedResponse> oauthLogin() throws ConnectionClosedException {
    return send(new XOAuth2Command(userName, authToken));
  }

  public Future<TaggedResponse> logout() throws ConnectionClosedException {
    return send(new BaseImapCommand(ImapCommandType.LOGOUT));
  }

  public Future<ListResponse> list(String context, String query) throws ConnectionClosedException {
    return send(new ListCommand(context, query));
  }

  public Future<OpenResponse> open(String folderName, boolean readOnly) throws ConnectionClosedException {
    return send(new OpenCommand(folderName, readOnly));
  }

  public Future<FetchResponse> fetch(long startId, Optional<Long> stopId, FetchDataItem fetchDataItem, FetchDataItem... otherFetchDataItems) throws ConnectionClosedException {
    return send(new FetchCommand(startId, stopId, fetchDataItem, otherFetchDataItems));
  }

  public Future<FetchResponse> fetch(long startId, Optional<Long> stopId, List<FetchDataItem> fetchItems) throws ConnectionClosedException {
    Preconditions.checkArgument(fetchItems.size() > 0, "Must have at least one FETCH item.");
    return send(new FetchCommand(startId, stopId, fetchItems));
  }

  public Future<StreamingFetchResponse> uidfetch(long startId, Optional<Long> stopId, Consumer<ImapMessage> messageConsumer, FetchDataItem item, FetchDataItem... otherItems) throws ConnectionClosedException {
    return send(new UidCommand(ImapCommandType.FETCH, new StreamingFetchCommand(startId, stopId, messageConsumer, item, otherItems)));
  }

  public Future<StreamingFetchResponse> fetch(long startId, Optional<Long> stopId, Consumer<ImapMessage> messageConsumer, FetchDataItem item, FetchDataItem... otherItems) throws ConnectionClosedException {
    return send(new StreamingFetchCommand(startId, stopId, messageConsumer, item, otherItems));
  }

  public Future<StreamingFetchResponse> fetch(long startId, Optional<Long> stopId, Consumer<ImapMessage> messageConsumer, List<FetchDataItem> fetchDataItems) throws ConnectionClosedException {
    Preconditions.checkArgument(fetchDataItems.size() > 0, "Must have at least one FETCH item.");
    return send(new StreamingFetchCommand(startId, stopId, messageConsumer, fetchDataItems));
  }

  public Future<FetchResponse> uidfetch(long startId, Optional<Long> stopId, FetchDataItem item, FetchDataItem... otherItems) throws ConnectionClosedException {
    return send(new UidCommand(ImapCommandType.FETCH, new FetchCommand(startId, stopId, item, otherItems)));
  }

  public Future<FetchResponse> uidfetch(long startId, Optional<Long> stopId, List<FetchDataItem> fetchItems) throws ConnectionClosedException {
    Preconditions.checkArgument(fetchItems.size() > 0, "Must have at least one FETCH item.");
    return send(new UidCommand(ImapCommandType.FETCH, new FetchCommand(startId, stopId, fetchItems)));
  }

  public Future<StreamingFetchResponse> uidfetch(long startId, Optional<Long> stopId, Consumer<ImapMessage> messageConsumer, List<FetchDataItem> fetchDataItems) throws ConnectionClosedException {
    Preconditions.checkArgument(fetchDataItems.size() > 0, "Must have at least one FETCH item.");
    return send(new UidCommand(ImapCommandType.FETCH, new StreamingFetchCommand(startId, stopId, messageConsumer, fetchDataItems)));
  }

  public Future<TaggedResponse> uidstore(StoreAction action, long startId, Optional<Long> stopId, MessageFlag... flags) throws ConnectionClosedException {
    return send(new UidCommand(ImapCommandType.STORE, new SilentStoreCommand(action, startId, stopId.orElse(startId), flags)));
  }

  public Future<SearchResponse> uidsearch(SearchKey... keys) throws ConnectionClosedException {
    return send(new UidCommand(ImapCommandType.SEARCH, new SearchCommand(keys)));
  }

  public Future<SearchResponse> uidsearch(SearchCommand cmd) throws ConnectionClosedException {
    return send(new UidCommand(ImapCommandType.SEARCH, cmd));
  }

  public Future<SearchResponse> search(SearchKey... keys) throws ConnectionClosedException {
    return send(new SearchCommand(keys));
  }

  public Future<SearchResponse> search(SearchCommand cmd) throws ConnectionClosedException {
    return send(cmd);
  }

  public Future<TaggedResponse> expunge() throws ConnectionClosedException {
    return send(ImapCommandType.EXPUNGE);
  }

  public Future<NoopResponse> noop() throws ConnectionClosedException {
    return send(ImapCommandType.NOOP);
  }

  public boolean isLoggedIn() {
    return loginPromise.isSuccess() && channel.isActive();
  }

  public boolean isConnected() {
    return channel != null && channel.isActive();
  }

  public boolean isClosed() {
    return connectionClosed.get();
  }

  public void awaitLogin() throws InterruptedException, ExecutionException {
    loginPromise.get();
  }

  public <T extends TaggedResponse> Future<T> send(ImapCommandType imapCommandType, String... args) throws ConnectionClosedException {
    BaseImapCommand baseImapCommand = new BaseImapCommand(imapCommandType, args);
    return send(baseImapCommand);
  }

  /**
   * Sends a command. If there is currently a command in progress, this command will be queued and executed when the currently running command finishes.
   * It is possible for a command to be queued and then a connection closed before it is actually executed, so it is important to listen to the returned future in order to ensure that the command was completed.
   *
   * @param imapCommand command to send
   * @param <T> Response type
   * @return Response future. Will be completed when a tagged response is received for this command.
   */
  public synchronized <T extends TaggedResponse> Future<T> send(ImapCommand imapCommand) throws ConnectionClosedException {
    final Promise<T> commandPromise = promiseExecutor.next().newPromise();
    commandPromise.addListener((f) -> {
      writeNext();
    });

    send(imapCommand, commandPromise);

    return commandPromise;
  }

  public synchronized void send(ImapCommand imapCommand, Promise promise) throws ConnectionClosedException {
    if (connectionClosed.get()) {
      throw new ConnectionClosedException("Cannot write to closed connection.");
    }

    if ((currentCommandPromise != null && !currentCommandPromise.isDone()) || !isConnected()) {
      PendingCommand pendingCommand = PendingCommand.newInstance(imapCommand, promise);
      pendingWriteQueue.add(pendingCommand);
    } else {
      actuallySend(imapCommand, promise);
    }
  }

  public void actuallySend(ImapCommand imapCommand, Promise promise) {
    currentCommandPromise = promise;

    clientState.setCurrentCommand(imapCommand);
    channel.writeAndFlush(imapCommand);
  }

  public synchronized void writeNext() throws ConnectionClosedException {
    if (pendingWriteQueue.peek() != null) {
      if (channel.isWritable()) {
        PendingCommand pendingCommand = pendingWriteQueue.poll();
        send(pendingCommand.imapCommand, pendingCommand.promise);

        pendingCommand.recycle();
      } else {
        channel.eventLoop().schedule(() -> {
          this.writeNext();
          return null;
        }, configuration.getWriteBackOffMs(), TimeUnit.MILLISECONDS);
      }
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ContinuationResponse) {
      currentCommandPromise.setSuccess(msg);
    } else if (msg instanceof TaggedResponse) {
      TaggedResponse taggedResponse = ((TaggedResponse) msg);

      if (currentCommandPromise.isDone() && !currentCommandPromise.isSuccess()) {
        LOGGER.debug("Got tagged response to failed imapCommand, skipping");
        return;
      }
      try {
        currentCommandPromise.setSuccess(taggedResponse);
      } catch (IllegalStateException e) {
        LOGGER.debug("Could not complete current imapCommand", e);
      }
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      if (!connectionClosed.get()) {
        idleExecutor.next().submit(() -> {
          noop();
          return null;
        });
      }
    } else if (evt instanceof ByeEvent) {
      if (channel.isOpen() && clientState.getCurrentCommand().getCommandType() != ImapCommandType.LOGOUT) {
        closeNow();
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
      int stepTimeoutSec = configuration.getCloseTimeoutSec() / 3;
      try {
        connectionClosed.set(true);
        if (currentCommandPromise != null && !currentCommandPromise.isDone()) {
          try {
            if (!currentCommandPromise.await(stepTimeoutSec, TimeUnit.SECONDS)) {
              pendingWriteQueue.iterator().forEachRemaining(c -> c.promise.setFailure(new ConnectionClosedException()));
              currentCommandPromise.cancel(true);
            }
          } catch (InterruptedException e) {
            LOGGER.error("Interrupted completing pending commands on close.", e);
            throw Throwables.propagate(e);
          }
        }

        Promise<TaggedResponse> logoutPromise = promiseExecutor.next().newPromise();
        actuallySend(new BaseImapCommand(ImapCommandType.LOGOUT), logoutPromise);
        try {
          logoutPromise.get(stepTimeoutSec, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          LOGGER.debug("Caught exception while closing client", e);
        }
      } finally {
        try {
          channel.close().get(stepTimeoutSec, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
          LOGGER.error("Exception closing channel.", e);
          throw Throwables.propagate(e);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.warn("Interrupted closing channel.", e);
        }
      }
    }
  }

  public void closeNow() {
    if (channel != null) {
      try {
        channel.close().get(configuration.getCloseTimeoutSec(), TimeUnit.SECONDS);
      } catch (ExecutionException | TimeoutException e) {
        LOGGER.error("Exception closing channel.", e);
        throw Throwables.propagate(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.warn("Interrupted closing channel.", e);
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

    private ImapCommand imapCommand;
    private Promise promise;

    public PendingCommand(Handle handle) {
      this.handle = handle;
    }

    static PendingCommand newInstance(ImapCommand imapCommand, Promise promise) {
      PendingCommand pendingCommand = RECYCLER.get();

      pendingCommand.imapCommand = imapCommand;
      pendingCommand.promise = promise;

      return pendingCommand;
    }

    private void recycle() {
      imapCommand = null;
      promise = null;
      RECYCLER.recycle(this, handle);
    }
  }
}
