package com.hubspot.imap;

import com.hubspot.imap.imap.Command;
import com.hubspot.imap.imap.CommandType;
import com.hubspot.imap.imap.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ImapClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapClient.class);

  private final Channel channel;
  private final EventExecutor executor;
  private final String userName;
  private final String password;

  private final AtomicInteger commandCount;
  private final CountDownLatch loginLatch;

  private final AtomicReference<Command> lastCommand;
  private Promise<Response> lastCommandPromise;

  public ImapClient(Channel channel, EventExecutor executor, String userName, String password) {
    this.channel = channel;
    this.channel.pipeline().addLast(new InboundHandler());
    this.channel.pipeline().addLast(new OutboundHandler());

    this.executor = executor;
    this.userName = userName;
    this.password = password;

    commandCount = new AtomicInteger(0);
    loginLatch = new CountDownLatch(1);
    lastCommand = new AtomicReference<>();
  }

  public Future<Response> login() {
    send(CommandType.LOGIN, userName, password);


    lastCommandPromise.addListener(future -> loginLatch.countDown());

    return lastCommandPromise;
  }

  public Future<Response> noop() {
    return send(CommandType.NOOP);
  }

  public boolean isLoggedIn() {
    return loginLatch.getCount() == 0 && channel.isOpen();
  }

  public void awaitLogin() throws InterruptedException {
    loginLatch.await();
  }

  public synchronized Future<Response> send(CommandType commandType, String... args) {
    if (lastCommandPromise != null) {
      lastCommandPromise.awaitUninterruptibly();
    }

    Command command = new Command(commandType, commandCount.getAndIncrement(), args);
    lastCommand.set(command);
    lastCommandPromise = executor.newPromise();
    channel.writeAndFlush(command);

    return lastCommandPromise;
  }

  private class InboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      Response response = ((Response) msg);
      if (!response.isAnonymous()) {
        if (response.getId().equalsIgnoreCase(lastCommand.get().getId())) {
          lastCommandPromise.setSuccess(response);
        }
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      LOGGER.error("Error in inbound handler", cause);
      super.exceptionCaught(ctx, cause);
    }
  }

  private class OutboundHandler extends ChannelOutboundHandlerAdapter {
  }
}
