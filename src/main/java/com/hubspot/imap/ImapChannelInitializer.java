package com.hubspot.imap;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;

import org.slf4j.Logger;

import com.google.common.net.HostAndPort;
import com.hubspot.imap.utils.LogUtils;
import com.hubspot.imap.utils.LoggingSocks4ProxyHandler;
import com.hubspot.imap.utils.SocksProxyLogger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;

@Sharable
public class ImapChannelInitializer extends ChannelInitializer<SocketChannel> {
  private static final StringEncoder STRING_ENCODER = new StringEncoder(Charset.forName("UTF-8"));

  private final SslContext sslContext;
  private final String clientName;
  private final ImapClientConfiguration configuration;

  public ImapChannelInitializer(SslContext sslContext, String clientName, ImapClientConfiguration configuration) {
    this.sslContext = sslContext;
    this.clientName = clientName;
    this.configuration = configuration;
  }

  public ImapChannelInitializer(String clientName, ImapClientConfiguration configuration) {
    this(null, clientName, configuration);
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) throws Exception {
    ChannelPipeline channelPipeline = socketChannel.pipeline();

    if (configuration.socksProxyConfig().isPresent()) {
      HostAndPort proxyHost = configuration.socksProxyConfig().get().proxyHost();
      channelPipeline.addLast(new LoggingSocks4ProxyHandler(clientName,
          new InetSocketAddress(proxyHost.getHost(), proxyHost.getPort())));
    }

    if (sslContext != null) {
      channelPipeline.addLast(sslContext.newHandler(socketChannel.alloc(),
        configuration.hostAndPort().getHost(),
        configuration.hostAndPort().getPortOrDefault(993)));
    }

    channelPipeline.addLast(STRING_ENCODER);
  }
}
