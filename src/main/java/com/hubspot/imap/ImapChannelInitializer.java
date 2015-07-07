package com.hubspot.imap;

import com.google.common.net.HostAndPort;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

import java.nio.charset.Charset;


public class ImapChannelInitializer extends ChannelInitializer<SocketChannel> {
  private static final StringDecoder STRING_DECODER = new StringDecoder(Charset.forName("UTF-8"));
  private static final StringEncoder STRING_ENCODER = new StringEncoder(Charset.forName("UTF-8"));
  private static final ImapCodec IMAP_CODEC = new ImapCodec();

  private final SslContext sslContext;
  private final HostAndPort hostAndPort;

  public ImapChannelInitializer(SslContext sslContext, HostAndPort hostAndPort) {
    this.sslContext = sslContext;
    this.hostAndPort = hostAndPort;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) throws Exception {
    ChannelPipeline channelPipeline = socketChannel.pipeline();

    channelPipeline.addLast(sslContext.newHandler(socketChannel.alloc(), hostAndPort.getHostText(), hostAndPort.getPortOrDefault(993)));
    channelPipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
    channelPipeline.addLast(STRING_DECODER);
    channelPipeline.addLast(STRING_ENCODER);
    channelPipeline.addLast(IMAP_CODEC);
  }
}
