package com.hubspot.imap;

import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;
import com.hubspot.imap.client.ImapClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class ImapClientFactory implements AutoCloseable {

  private final ImapConfiguration configuration;
  private final HostAndPort hostAndPort;
  private final Bootstrap bootstrap;
  private final EventLoopGroup eventLoopGroup;
  private final EventExecutorGroup eventExecutor;
  private final SslContext context;

  public ImapClientFactory(ImapConfiguration configuration) {
    this.configuration = configuration;
    this.hostAndPort = configuration.getHostAndPort();
    this.bootstrap = new Bootstrap();
    this.eventLoopGroup = new NioEventLoopGroup();
    this.eventExecutor = new DefaultEventExecutorGroup(16);

    try {
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(((KeyStore) null));

      this.context = SslContextBuilder.forClient()
          .trustManager(trustManagerFactory)
          .build();
    } catch (NoSuchAlgorithmException |SSLException |KeyStoreException e) {
      throw Throwables.propagate(e);
    }

    bootstrap.group(eventLoopGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .handler(new ImapChannelInitializer(context, hostAndPort));
  }

  public ImapClient connect(String userName, String oauthToken) throws InterruptedException {
    Channel channel = bootstrap.connect(hostAndPort.getHostText(), hostAndPort.getPort()).sync().channel();
    return new ImapClient(configuration, channel, eventExecutor, userName, oauthToken);
  }

  @Override
  public void close() {
    eventExecutor.shutdownGracefully();
    eventLoopGroup.shutdownGracefully();
  }
}
