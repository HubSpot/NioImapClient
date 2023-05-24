package com.hubspot.imap.utils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4ClientDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ClientEncoder;
import io.netty.handler.codec.socksx.v4.Socks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import io.netty.handler.proxy.ProxyConnectException;
import io.netty.handler.proxy.ProxyHandler;

/**
 * This is basically a copy of the netty Socks4ProxyHandler that simply logs the IP address
 * that the socks proxy server responds with upon connection.  This allows us to log
 * the IP address of the server that is performing the connection, rather than simply the
 * cluster IP in the case of a socks proxy cluster. Admittedly, the netty socks proxy
 * implementation is broken here since it doesn't make this important information more readily
 * available.
 */
public final class LoggingSocks4ProxyHandler extends ProxyHandler {
  static final String AUTH_NONE = "none";

  private static final String PROTOCOL = "socks4";
  private static final String AUTH_USERNAME = "username";

  private final String username;
  private final Logger logger;

  private String decoderName;
  private String encoderName;

  public LoggingSocks4ProxyHandler(String logContext, SocketAddress proxyAddress) {
    this(logContext, proxyAddress, null);
  }

  public LoggingSocks4ProxyHandler(String logContext, SocketAddress proxyAddress, String username) {
    super(proxyAddress);
    if (username != null && username.length() == 0) {
      username = null;
    }
    this.username = username;
    this.logger = LogUtils.loggerWithName(LoggingSocks4ProxyHandler.class, logContext);
  }

  @Override
  public String protocol() {
    return PROTOCOL;
  }

  @Override
  public String authScheme() {
    return username != null? AUTH_USERNAME : AUTH_NONE;
  }

  public String username() {
    return username;
  }

  @Override
  protected void addCodec(ChannelHandlerContext ctx) throws Exception {
    ChannelPipeline p = ctx.pipeline();
    String name = ctx.name();

    Socks4ClientDecoder decoder = new Socks4ClientDecoder();
    p.addBefore(name, null, decoder);

    decoderName = p.context(decoder).name();
    encoderName = decoderName + ".encoder";

    p.addBefore(name, encoderName, Socks4ClientEncoder.INSTANCE);
  }

  @Override
  protected void removeEncoder(ChannelHandlerContext ctx) throws Exception {
    ChannelPipeline p = ctx.pipeline();
    p.remove(encoderName);
  }

  @Override
  protected void removeDecoder(ChannelHandlerContext ctx) throws Exception {
    ChannelPipeline p = ctx.pipeline();
    p.remove(decoderName);
  }

  @Override
  protected Object newInitialMessage(ChannelHandlerContext ctx) throws Exception {
    InetSocketAddress raddr = destinationAddress();
    String rhost;
    if (raddr.isUnresolved()) {
      rhost = raddr.getHostString();
    } else {
      rhost = raddr.getAddress().getHostAddress();
    }
    return new DefaultSocks4CommandRequest(
        Socks4CommandType.CONNECT, rhost, raddr.getPort(), username != null? username : "");
  }

  @Override
  protected boolean handleResponse(ChannelHandlerContext ctx, Object response) throws Exception {
    final Socks4CommandResponse res = (Socks4CommandResponse) response;
    logger.info("Connected to SOCKS proxy with IP address {}", res.dstAddr());
    final Socks4CommandStatus status = res.status();
    if (status == Socks4CommandStatus.SUCCESS) {
      return true;
    }

    throw new ProxyConnectException(exceptionMessage("status: " + status));
  }
}
