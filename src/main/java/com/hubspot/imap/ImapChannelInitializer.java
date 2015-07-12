package com.hubspot.imap;

import com.google.common.net.HostAndPort;
import com.hubspot.imap.imap.ResponseDecoder;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.charset.Charset;

@Sharable
public class ImapChannelInitializer extends ChannelInitializer<SocketChannel> {
  private static final StringEncoder STRING_ENCODER = new StringEncoder(Charset.forName("UTF-8"));

  private final SslContext sslContext;
  private final HostAndPort hostAndPort;

  private final int maxIdleTimeSeconds;

  public ImapChannelInitializer(SslContext sslContext, HostAndPort hostAndPort, int maxIdleTimeSeconds) {
    this.sslContext = sslContext;
    this.hostAndPort = hostAndPort;
    this.maxIdleTimeSeconds = maxIdleTimeSeconds;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) throws Exception {
    ChannelPipeline channelPipeline = socketChannel.pipeline();

    channelPipeline.addLast(sslContext.newHandler(socketChannel.alloc(), hostAndPort.getHostText(), hostAndPort.getPortOrDefault(993)));
    channelPipeline.addLast(new ResponseDecoder());
    //channelPipeline.addLast(new ImapResponseDecoder());
    channelPipeline.addLast(STRING_ENCODER);
    channelPipeline.addLast(new IdleStateHandler(maxIdleTimeSeconds, maxIdleTimeSeconds, maxIdleTimeSeconds));
  }
}
