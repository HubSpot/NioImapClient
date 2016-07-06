package com.hubspot.imap.client;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
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
import com.hubspot.imap.protocol.command.fetch.SetFetchCommand;
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
import com.hubspot.imap.utils.LogUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public class ImapClient extends ChannelDuplexHandler implements AutoCloseable, Closeable {

  private static final String KEEP_ALIVE_HANDLER = "imap noop keep alive";

  private final Logger logger;
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

  public ImapClient(ImapConfiguration configuration,
                    Bootstrap bootstrap,
                    EventExecutorGroup promiseExecutor,
                    EventExecutorGroup idleExecutor,
                    String clientName,
                    String userName,
                    String authToken) {
    this.logger = LogUtils.loggerWithName(ImapClient.class, clientName);
    this.configuration = configuration;
    this.bootstrap = bootstrap;
    this.promiseExecutor = promiseExecutor;
    this.idleExecutor = idleExecutor;
    this.userName = userName;
    this.authToken = authToken;
    this.clientState = new ImapClientState(clientName, promiseExecutor);
    this.codec = new ImapCodec(clientState);
    this.pendingWriteQueue = new ConcurrentLinkedQueue<>();
    this.connectionClosed = new AtomicBoolean(false);

    loginPromise = promiseExecutor.next().newPromise();
  }

  public synchronized ChannelFuture connect() {
    ChannelFuture future = bootstrap.connect(configuration.hostAndPort().getHostText(),
        configuration.hostAndPort().getPort());

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
    this.channel.pipeline()
        .addLast(new ReadTimeoutHandler(configuration.socketTimeoutMs(), TimeUnit.MILLISECONDS))
        .addLast(idleExecutor, new ResponseDecoder(configuration, clientState, promiseExecutor))
        .addLast(idleExecutor, codec)
        .addLast(idleExecutor, this)
        .addLast(promiseExecutor, this.clientState);
  }

  public ImapClientState getState() {
    return clientState;
  }

  public Future<TaggedResponse> login() {
    Future<TaggedResponse> loginFuture;
    switch (configuration.authType()) {
      case XOAUTH2:
        loginFuture = oauthLogin();
        break;
      default:
        loginFuture = passwordLogin();
        break;
    }

    loginFuture.addListener(future -> {
      final Object response = future.get();
      if (response instanceof ContinuationResponse) {
        send(ImapCommandType.BLANK).addListener(blankFuture -> {
          String continuationMessage = ((ContinuationResponse) response).getMessage();
          Object blankResponse = blankFuture.get();
          if (blankResponse instanceof TaggedResponse) {
            loginPromise.tryFailure(AuthenticationFailedException.fromContinuation(((TaggedResponse) blankResponse).getMessage(), continuationMessage));
          } else {
            loginPromise.tryFailure(AuthenticationFailedException.fromContinuation(continuationMessage));
          }
        });
      } else {
        TaggedResponse taggedResponse = ((TaggedResponse) response);
        if (taggedResponse.getCode() == ResponseCode.BAD) {
          loginPromise.tryFailure(new AuthenticationFailedException(taggedResponse.getMessage()));
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
    int keepAliveInterval = configuration.noopKeepAliveIntervalSec();
    if (keepAliveInterval > 0) {
      if (!connectionClosed.get() && channel.pipeline().get(KEEP_ALIVE_HANDLER) == null) {
        this.channel.pipeline().addFirst(KEEP_ALIVE_HANDLER, new IdleStateHandler(keepAliveInterval, keepAliveInterval, keepAliveInterval));
      }
    }
  }

  private Future<TaggedResponse> passwordLogin() {
    return send(new BaseImapCommand(ImapCommandType.LOGIN, userName, authToken));
  }

  private Future<TaggedResponse> oauthLogin() {
    return send(new XOAuth2Command(userName, authToken));
  }

  public Future<TaggedResponse> logout() {
    return send(new BaseImapCommand(ImapCommandType.LOGOUT));
  }

  public Future<ListResponse> list(String context, String query) {
    return send(new ListCommand(context, query));
  }

  public Future<OpenResponse> open(String folderName, FolderOpenMode openMode) {
    return send(new OpenCommand(folderName, openMode));
  }

  public Future<FetchResponse> fetch(long startId,
                                     Optional<Long> stopId,
                                     FetchDataItem fetchDataItem,
                                     FetchDataItem... otherFetchDataItems) {
    return send(new FetchCommand(startId, stopId, fetchDataItem, otherFetchDataItems));
  }

  public Future<FetchResponse> fetch(long startId, Optional<Long> stopId, List<FetchDataItem> fetchItems) {
    Preconditions.checkArgument(fetchItems.size() > 0, "Must have at least one FETCH item.");
    return send(new FetchCommand(startId, stopId, fetchItems));
  }

  public Future<StreamingFetchResponse> uidfetch(long startId,
                                                 Optional<Long> stopId,
                                                 Consumer<ImapMessage> messageConsumer,
                                                 FetchDataItem item,
                                                 FetchDataItem... otherItems) {
    return send(new UidCommand(ImapCommandType.FETCH, new StreamingFetchCommand(startId, stopId, messageConsumer, item, otherItems)));
  }

  public Future<StreamingFetchResponse> fetch(long startId,
                                              Optional<Long> stopId,
                                              Consumer<ImapMessage> messageConsumer,
                                              FetchDataItem item,
                                              FetchDataItem... otherItems) {
    return send(new StreamingFetchCommand(startId, stopId, messageConsumer, item, otherItems));
  }

  public Future<StreamingFetchResponse> fetch(long startId,
                                              Optional<Long> stopId,
                                              Consumer<ImapMessage> messageConsumer,
                                              List<FetchDataItem> fetchDataItems) {
    Preconditions.checkArgument(fetchDataItems.size() > 0, "Must have at least one FETCH item.");
    return send(new StreamingFetchCommand(startId, stopId, messageConsumer, fetchDataItems));
  }

  public Future<FetchResponse> uidfetch(long startId,
                                        Optional<Long> stopId,
                                        FetchDataItem item,
                                        FetchDataItem... otherItems) {
    return send(new UidCommand(ImapCommandType.FETCH, new FetchCommand(startId, stopId, item, otherItems)));
  }

  public Future<FetchResponse> uidfetch(Set<Long> uids, FetchDataItem first, FetchDataItem... others) {
    return uidfetch(uids, Lists.asList(first, others));
  }

  public Future<FetchResponse> uidfetch(Set<Long> uids, List<FetchDataItem> items) {
    return send(new UidCommand(ImapCommandType.FETCH, new SetFetchCommand(uids, items)));
  }

  public Future<FetchResponse> uidfetch(long startId, Optional<Long> stopId, List<FetchDataItem> fetchItems) {
    Preconditions.checkArgument(fetchItems.size() > 0, "Must have at least one FETCH item.");
    return send(new UidCommand(ImapCommandType.FETCH, new FetchCommand(startId, stopId, fetchItems)));
  }

  public Future<StreamingFetchResponse> uidfetch(long startId,
                                                 Optional<Long> stopId,
                                                 Consumer<ImapMessage> messageConsumer,
                                                 List<FetchDataItem> fetchDataItems) {
    Preconditions.checkArgument(fetchDataItems.size() > 0, "Must have at least one FETCH item.");
    return send(new UidCommand(ImapCommandType.FETCH, new StreamingFetchCommand(startId, stopId, messageConsumer, fetchDataItems)));
  }

  public Future<TaggedResponse> uidstore(StoreAction action,
                                         long startId,
                                         Optional<Long> stopId,
                                         MessageFlag... flags) {
    return send(new UidCommand(ImapCommandType.STORE, new SilentStoreCommand(action, startId, stopId.orElse(startId), flags)));
  }

  public Future<SearchResponse> uidsearch(SearchKey... keys) {
    return send(new UidCommand(ImapCommandType.SEARCH, new SearchCommand(keys)));
  }

  public Future<SearchResponse> uidsearch(SearchCommand cmd) {
    return send(new UidCommand(ImapCommandType.SEARCH, cmd));
  }

  public Future<SearchResponse> search(SearchKey... keys) {
    return send(new SearchCommand(keys));
  }

  public Future<SearchResponse> search(SearchCommand cmd) {
    return send(cmd);
  }

  public Future<TaggedResponse> expunge() {
    return send(ImapCommandType.EXPUNGE);
  }

  public Future<NoopResponse> noop() {
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

  public <T extends TaggedResponse> Future<T> send(ImapCommandType imapCommandType, String... args) {
    BaseImapCommand baseImapCommand = new BaseImapCommand(imapCommandType, args);
    return send(baseImapCommand);
  }

  /**
   * Sends a command. If there is currently a command in progress, this command will be queued and executed when the currently running command finishes.
   * It is possible for a command to be queued and then a connection closed before it is actually executed, so it is important to listen to the returned future in order to ensure that the command was completed.
   *
   * @param imapCommand command to send
   * @param <T>         Response type
   * @return Response future. Will be completed when a tagged response is received for this command.
   */
  public synchronized <T extends TaggedResponse> Future<T> send(ImapCommand imapCommand) {
    final Promise<T> commandPromise = promiseExecutor.next().newPromise();
    commandPromise.addListener((f) -> {
      writeNext();
    });

    send(imapCommand, commandPromise);

    return commandPromise;
  }

  public synchronized void send(ImapCommand imapCommand, Promise promise) {
    if (connectionClosed.get()) {
      promise.tryFailure(new ConnectionClosedException("Cannot write to closed connection."));
      return;
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
    if (connectionClosed.get()) {
      return;
    }

    if (pendingWriteQueue.peek() != null) {
      if (channel.isWritable()) {
        PendingCommand pendingCommand = pendingWriteQueue.poll();
        send(pendingCommand.imapCommand, pendingCommand.promise);

        pendingCommand.recycle();
      } else {
        channel.eventLoop().schedule(() -> {
          this.writeNext();
          return null;
        }, configuration.writeBackOffMs(), TimeUnit.MILLISECONDS);
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
        logger.debug("Got tagged response to failed imapCommand, skipping");
        return;
      }
      try {
        currentCommandPromise.setSuccess(taggedResponse);
      } catch (IllegalStateException e) {
        logger.debug("Could not complete current imapCommand", e);
      }
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      if (!connectionClosed.get()) {
        noop();
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
    if (currentCommandPromise != null) {
      logger.debug("Error while executing {}", clientState.getCurrentCommand().getCommandType(), cause);
      currentCommandPromise.tryFailure(cause);
    } else {
      logger.error("Error in handler", cause);
      ctx.pipeline().fireExceptionCaught(cause);
    }
  }

  public Future closeAsync() {
    if (isConnected() && !connectionClosed.get()) {
      connectionClosed.set(true);
      if (currentCommandPromise != null && !currentCommandPromise.isDone()) {
        currentCommandPromise.cancel(true);
      }

      return sendLogout();
    } else {
      return idleExecutor.next().submit(this::closeNow);
    }
  }

  private Future sendLogout() {
    Promise<TaggedResponse> logoutPromise = promiseExecutor.next().newPromise();
    actuallySend(new BaseImapCommand(ImapCommandType.LOGOUT), logoutPromise);

    return logoutPromise.addListener(future1 -> closeNow());
  }

  public void closeNow() {
    connectionClosed.set(true);
    if (channel != null && channel.isOpen()) {
      try {
        channel.close().get(configuration.closeTimeoutSec(), TimeUnit.SECONDS);
      } catch (ExecutionException | TimeoutException e) {
        logger.error("Exception closing channel.", e);
        throw Throwables.propagate(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.warn("Interrupted closing channel.", e);
      }
    }
  }

  @Override
  public void close() {
    try {
      closeAsync().get(configuration.closeTimeoutSec(), TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error("Caught exception while closing client!", e);
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
