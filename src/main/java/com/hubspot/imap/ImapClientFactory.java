package com.hubspot.imap;

import java.io.Closeable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.hubspot.imap.client.ImapClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;


public class ImapClientFactory implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapClientFactory.class);

  private final ImapClientFactoryConfiguration configuration;
  private final SslContext sslContext;

  public ImapClientFactory(ImapClientFactoryConfiguration configuration) {
    this.configuration = configuration;

    try {
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(((KeyStore) null));

      sslContext = SslContextBuilder.forClient()
          .trustManager(trustManagerFactory)
          .build();
    } catch (NoSuchAlgorithmException | SSLException | KeyStoreException e) {
      throw Throwables.propagate(e);
    }
  }

  private ImapClient create(String clientName, Channel channel, ImapClientFactoryConfiguration configuration) {
    return new ImapClient(configuration, channel, sslContext, configuration.executor(), clientName);
  }

  public CompletableFuture<ImapClient> connect() {
    return connect(UUID.randomUUID().toString());
  }

  public CompletableFuture<ImapClient> connect(String clientName) {
    return connect(clientName, Optional.empty());
  }

  public CompletableFuture<ImapClient> connect(String clientName, Optional<ImapClientFactoryConfiguration> maybeConfigOverride) {
    ImapClientFactoryConfiguration configuration = maybeConfigOverride.orElse(this.configuration);

    Bootstrap bootstrap = new Bootstrap().group(configuration.eventLoopGroup())
        .option(ChannelOption.SO_LINGER, configuration.soLinger())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectTimeoutMillis())
        .option(ChannelOption.SO_KEEPALIVE, false)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .handler(configuration.useSsl() ? new ImapChannelInitializer(sslContext, configuration) : new ImapChannelInitializer(configuration))
        .channel(configuration.channelClass());

    CompletableFuture<ImapClient> connectFuture = new CompletableFuture<>();

    bootstrap.connect(configuration.hostAndPort().getHostText(), configuration.hostAndPort().getPort()).addListener(f -> {
      if (f.isSuccess()) {
        Channel channel = ((ChannelFuture) f).channel();

        ImapClient client = create(clientName, channel, configuration);
        configuration.executor().execute(() -> {
          connectFuture.complete(client);
        });
      } else {
        configuration.executor().execute(() -> connectFuture.completeExceptionally(f.cause()));
      }
    });

    return connectFuture;
  }

  @Override
  public void close() {
    configuration.eventLoopGroup().shutdownGracefully();
    configuration.executor().shutdownGracefully();
  }
}
