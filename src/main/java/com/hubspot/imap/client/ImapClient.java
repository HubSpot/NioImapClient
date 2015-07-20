package com.hubspot.imap.client;

import com.google.seventeen.common.base.Throwables;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.imap.command.BaseCommand;
import com.hubspot.imap.imap.command.BlankCommand;
import com.hubspot.imap.imap.command.Command;
import com.hubspot.imap.imap.command.CommandType;
import com.hubspot.imap.imap.command.ListCommand;
import com.hubspot.imap.imap.command.OpenCommand;
import com.hubspot.imap.imap.command.XOAuth2Command;
import com.hubspot.imap.imap.command.fetch.FetchCommand;
import com.hubspot.imap.imap.command.fetch.items.FetchDataItem;
import com.hubspot.imap.imap.exceptions.AuthenticationFailedException;
import com.hubspot.imap.imap.response.ContinuationResponse;
import com.hubspot.imap.imap.response.ResponseCode;
import com.hubspot.imap.imap.response.events.ByeEvent;
import com.hubspot.imap.imap.response.tagged.FetchResponse;
import com.hubspot.imap.imap.response.tagged.ListResponse;
import com.hubspot.imap.imap.response.tagged.NoopResponse;
import com.hubspot.imap.imap.response.tagged.OpenResponse;
import com.hubspot.imap.imap.response.tagged.TaggedResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ImapClient extends ChannelDuplexHandler implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapClient.class);

  private final ImapConfiguration configuration;
  private final Channel channel;
  private final EventExecutorGroup executorGroup;
  private final String userName;
  private final String authToken;
  private final ImapClientState clientState;

  private final Promise<Void> loginPromise;

  private Promise lastCommandPromise;

  public ImapClient(ImapConfiguration configuration, Channel channel, EventExecutorGroup executorGroup, String userName, String authToken) {
    this.configuration = configuration;
    this.channel = channel;

    this.executorGroup = executorGroup;
    this.userName = userName;
    this.authToken = authToken;
    this.clientState = new ImapClientState(executorGroup);

    loginPromise = executorGroup.next().newPromise();

    this.channel.pipeline().addLast(new ImapCodec(clientState));
    this.channel.pipeline().addLast(this);
    this.channel.pipeline().addLast(executorGroup, this.clientState);
  }

  public ImapClientState getState() {
    return clientState;
  }

  public Future<TaggedResponse> login() {
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
      if (!future.isSuccess()) {
        send(BlankCommand.INSTANCE);
      } else {
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

  private Future<TaggedResponse> passwordLogin() {
    return send(new BaseCommand(CommandType.LOGIN, userName, authToken));
  }

  private Future<TaggedResponse> oauthLogin() {
    return send(new XOAuth2Command(userName, authToken));
  }

  public Future<TaggedResponse> logout() {
    return send(new BaseCommand(CommandType.LOGOUT));
  }

  public Future<ListResponse> list(String context, String query) {
    return send(new ListCommand(context, query));
  }

  public Future<OpenResponse> open(String folderName, boolean readOnly) {
    return send(new OpenCommand(folderName, readOnly));
  }

  public Future<FetchResponse> fetch(long startId, Optional<Long> stopId, FetchDataItem... fetchDataItems) {
    return send(new FetchCommand(startId, stopId, fetchDataItems));
  }

  public Future<NoopResponse> noop() {
    return send(CommandType.NOOP);
  }

  public boolean isLoggedIn() {
    return loginPromise.isSuccess() && channel.isOpen();
  }

  public boolean isOpen() {
    return channel.isOpen();
  }

  public void awaitLogin() throws InterruptedException, ExecutionException {
    loginPromise.get();
  }

  public <T extends TaggedResponse> Future<T> send(CommandType commandType, String... args) {
    BaseCommand baseCommand = new BaseCommand(commandType, args);
    return send(baseCommand);
  }

  /**
   * Sends a command.
   *
   * This method will wait for any currently running command to finish before executing, this is a blocking operation.
   *
   * NOTE: It is critical that this method never be called from the event loop. This should not be a problem for user level application, but internal operations like keep-alive must be very careful to use separate executors when calling this method.
   * @param command Command to send
   * @param <T> Response type
   * @return Response future. Will be completed when a tagged response is received for this command.
   */
  public synchronized <T extends TaggedResponse> Future<T> send(Command command) {
    final Promise<T> newPromise = executorGroup.next().newPromise();
    if (lastCommandPromise != null) {
      try {
        lastCommandPromise.await();
      } catch (InterruptedException e) {
        throw Throwables.propagate(e);
      }
    }

    lastCommandPromise = newPromise;

    clientState.setCurrentCommand(command);
    channel.writeAndFlush(command);

    return newPromise;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ContinuationResponse) {
      lastCommandPromise.setSuccess(msg);
    } else if (msg instanceof TaggedResponse) {
      TaggedResponse taggedResponse = ((TaggedResponse) msg);

      try {
        lastCommandPromise.setSuccess(taggedResponse);
      } catch (IllegalStateException e) {
        LOGGER.debug("Could not complete current command", e);
      }
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      executorGroup.next().execute(() -> noop());
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
    if (lastCommandPromise != null) {
      lastCommandPromise.setFailure(cause);
    } else {
      ctx.fireExceptionCaught(cause);
    }
  }

  @Override
  public void close() {
    if (isOpen()) {
      if (lastCommandPromise != null && !lastCommandPromise.isDone()) {
        try {
          if (!lastCommandPromise.await(10, TimeUnit.SECONDS)) {
            lastCommandPromise.cancel(true);
          }
        } catch (InterruptedException e) {
          throw Throwables.propagate(e);
        }
      }

      Future<TaggedResponse> logoutFuture = logout();
      try {
        logoutFuture.get(10, TimeUnit.SECONDS);
      } catch (InterruptedException|ExecutionException|TimeoutException e) {
        LOGGER.error("Caught exception while closing client", e);
      } finally {
        channel.close();
      }
    }
  }
}
