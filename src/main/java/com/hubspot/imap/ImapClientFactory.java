package com.hubspot.imap;

import java.io.Closeable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.net.HostAndPort;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.ProxyCommand;

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

  public ImapClientFactory() {
    this(ImapClientFactoryConfiguration.builder().build());
  }

  public ImapClientFactory(ImapClientFactoryConfiguration configuration) {
    this(configuration, (KeyStore)null);
  }

  public ImapClientFactory(ImapClientFactoryConfiguration configuration, KeyStore keyStore) {
    this.configuration = configuration;

    try {
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keyStore);

      sslContext = SslContextBuilder.forClient()
          .trustManager(trustManagerFactory)
          .build();
    } catch (NoSuchAlgorithmException | SSLException | KeyStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public CompletableFuture<ImapClient> connect(ImapClientConfiguration clientConfiguration) {
    return connect("unknown-client", clientConfiguration);
  }

  public CompletableFuture<ImapClient> connect(String clientName, ImapClientConfiguration clientConfiguration) {
    Supplier<SslContext> sslContextSupplier = Suppliers.memoize(() -> getSslContext(clientConfiguration));
    boolean useSsl = clientConfiguration.useSsl() && !clientConfiguration.proxyConfig().isPresent();

    Bootstrap bootstrap = new Bootstrap().group(configuration.eventLoopGroup())
        .option(ChannelOption.SO_LINGER, clientConfiguration.soLinger())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfiguration.connectTimeoutMillis())
        .option(ChannelOption.SO_KEEPALIVE, false)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .channel(configuration.channelClass())
        .handler(useSsl
            ? new ImapChannelInitializer(sslContextSupplier.get(), clientName, clientConfiguration)
            : new ImapChannelInitializer(clientName, clientConfiguration));

    HostAndPort connectHost = getConnectHost(clientConfiguration);

    CompletableFuture<ImapClient> connectFuture = new CompletableFuture<>();


    bootstrap.connect(connectHost.getHost(), connectHost.getPort()).addListener(f -> {
      if (f.isSuccess()) {
        Channel channel = ((ChannelFuture) f).channel();

        ImapClient client = new ImapClient(clientConfiguration, channel, sslContextSupplier, configuration.executor(), clientName);
        configuration.executor().execute(() -> {
          connectFuture.complete(client);
        });
      } else {
        configuration.executor().execute(() -> connectFuture.completeExceptionally(f.cause()));
      }
    });

    return handleProxyConnect(clientConfiguration, connectFuture);
  }

  private SslContext getSslContext(ImapClientConfiguration clientConfiguration) {
    if (!clientConfiguration.trustManagerFactory().isPresent()) {
      return sslContext;
    }
    try {
      return SslContextBuilder.forClient()
          .trustManager(clientConfiguration.trustManagerFactory().get())
          .build();
    } catch (SSLException e) {
      throw new RuntimeException(e);
    }
  }

  private CompletableFuture<ImapClient> handleProxyConnect(ImapClientConfiguration clientConfiguration,
                                                           CompletableFuture<ImapClient> connectFuture) {
    if (!clientConfiguration.proxyConfig().isPresent()) {
      return connectFuture;
    }
    ProxyCommand proxyCommand = new ProxyCommand(
        clientConfiguration.hostAndPort(),
        clientConfiguration.proxyConfig().get().proxyLocalIpAddress()
    );
    return connectFuture.thenCompose(imapClient ->
        imapClient.send(proxyCommand)
            .thenApply(ignored -> {
              if (clientConfiguration.useSsl()) {
                imapClient.addTlsToChannel();
              }
              return imapClient;
            })
    );
  }

  private HostAndPort getConnectHost(ImapClientConfiguration clientConfiguration) {
    if (clientConfiguration.proxyConfig().isPresent()) {
      return clientConfiguration.proxyConfig().get().proxyHost();
    } else {
      return clientConfiguration.hostAndPort();
    }
  }

  @Override
  public void close() {
    configuration.eventLoopGroup().shutdownGracefully();
    configuration.executor().shutdownGracefully();
  }
}
