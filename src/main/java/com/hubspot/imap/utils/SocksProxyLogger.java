package com.hubspot.imap.utils;


import java.net.InetSocketAddress;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.proxy.Socks4ProxyHandler;

public class SocksProxyLogger extends ChannelInboundHandlerAdapter {
  private Logger logger;

  public SocksProxyLogger(String clientName) {
    this.logger = LogUtils.loggerWithName(SocksProxyLogger.class, clientName);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    Socks4ProxyHandler proxyHandler = ctx.channel().pipeline().get(Socks4ProxyHandler.class);
    InetSocketAddress proxyAddress = proxyHandler.proxyAddress();
    String hostAddress = proxyAddress.getAddress().getHostAddress();
    logger.info("Using SOCKS proxy at {}", hostAddress);
  }
}
