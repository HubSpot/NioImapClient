package com.hubspot.imap;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import com.google.common.net.HostAndPort;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.ssl.SslContext;

@Sharable
public class ImapChannelInitializer extends ChannelInitializer<SocketChannel> {
  private static final StringEncoder STRING_ENCODER = new StringEncoder(Charset.forName("UTF-8"));

  private final SslContext sslContext;
  private final ImapClientConfiguration configuration;

  public ImapChannelInitializer(SslContext sslContext, ImapClientConfiguration configuration) {
    this.sslContext = sslContext;
    this.configuration = configuration;
  }

  public ImapChannelInitializer(ImapClientConfiguration configuration) {
    this(null, configuration);
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) throws Exception {
    ChannelPipeline channelPipeline = socketChannel.pipeline();

    if (configuration.socksProxyConfig().isPresent()) {
      HostAndPort proxyHost = configuration.socksProxyConfig().get().proxyHost();
      channelPipeline.addFirst(new Socks4ProxyHandler(
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
