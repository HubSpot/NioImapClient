package com.hubspot.imap;

import java.nio.charset.Charset;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

@Sharable
public class ImapChannelInitializer extends ChannelInitializer<SocketChannel> {
  private static final StringEncoder STRING_ENCODER = new StringEncoder(Charset.forName("UTF-8"));

  private final SslContext sslContext;
  private final ImapConfiguration configuration;

  public ImapChannelInitializer(SslContext sslContext, ImapConfiguration configuration) {
    this.sslContext = sslContext;
    this.configuration = configuration;
  }

  public ImapChannelInitializer(ImapConfiguration configuration) {
    this(null, configuration);
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) throws Exception {
    ChannelPipeline channelPipeline = socketChannel.pipeline();

    if (sslContext != null) {
      channelPipeline.addLast(sslContext.newHandler(socketChannel.alloc(),
        configuration.hostAndPort().getHostText(),
        configuration.hostAndPort().getPortOrDefault(993)));
    }

    channelPipeline.addLast(STRING_ENCODER);
  }
}
