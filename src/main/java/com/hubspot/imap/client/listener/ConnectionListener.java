package com.hubspot.imap.client.listener;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;

public interface ConnectionListener extends ChannelInboundHandler {
  void connected();
  void disconnected();

  abstract class ConnectionListenerAdapter
    extends ChannelInboundHandlerAdapter
    implements ConnectionListener {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      connected();
      super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      disconnected();
      super.channelInactive(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      if (ctx.channel().isActive()) {
        connected();
      }
      super.handlerAdded(ctx);
    }
  }
}
