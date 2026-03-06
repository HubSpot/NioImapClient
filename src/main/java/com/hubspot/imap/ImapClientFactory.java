package com.hubspot.imap;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.net.HostAndPort;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.ProxyCommand;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.Closeable;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImapClientFactory implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImapClientFactory.class);

  private final ImapClientFactoryConfiguration configuration;
  private final TrustManagerFactory defaultTrustManagerFactory;
  private final SslContext sslContext;

  public ImapClientFactory() {
    this(ImapClientFactoryConfiguration.builder().build());
  }

  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public ImapClientFactory(ImapClientFactoryConfiguration configuration) {
    this(configuration, (KeyStore) null);
  }

  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public ImapClientFactory(
    ImapClientFactoryConfiguration configuration,
    KeyStore keyStore
  ) {
    this.configuration = configuration;

    try {
      defaultTrustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      defaultTrustManagerFactory.init(keyStore);

      sslContext =
        SslContextBuilder.forClient().trustManager(defaultTrustManagerFactory).build();
    } catch (NoSuchAlgorithmException | SSLException | KeyStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public CompletableFuture<ImapClient> connect(
    ImapClientConfiguration clientConfiguration
  ) {
    return connect("unknown-client", clientConfiguration);
  }

  public CompletableFuture<ImapClient> connect(
    String clientName,
    ImapClientConfiguration clientConfiguration
  ) {
    Supplier<SslContext> sslContextSupplier = Suppliers.memoize(() ->
      getSslContext(clientConfiguration)
    );
    boolean useSsl =
      clientConfiguration.useSsl() && !clientConfiguration.proxyConfig().isPresent();

    Bootstrap bootstrap = new Bootstrap()
      .group(configuration.eventLoopGroup())
      .option(ChannelOption.SO_LINGER, clientConfiguration.soLinger())
      .option(
        ChannelOption.CONNECT_TIMEOUT_MILLIS,
        clientConfiguration.connectTimeoutMillis()
      )
      .option(ChannelOption.SO_KEEPALIVE, false)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .channel(configuration.channelClass())
      .handler(
        useSsl
          ? new ImapChannelInitializer(
            sslContextSupplier.get(),
            clientName,
            clientConfiguration
          )
          : new ImapChannelInitializer(clientName, clientConfiguration)
      );

    HostAndPort connectHost = getConnectHost(clientConfiguration);

    CompletableFuture<ImapClient> connectFuture = new CompletableFuture<>();

    bootstrap
      .connect(connectHost.getHost(), connectHost.getPort())
      .addListener(f -> {
        if (f.isSuccess()) {
          Channel channel = ((ChannelFuture) f).channel();

          ImapClient client = new ImapClient(
            clientConfiguration,
            channel,
            sslContextSupplier,
            configuration.executor(),
            clientName
          );
          configuration
            .executor()
            .execute(() -> {
              connectFuture.complete(client);
            });
        } else {
          configuration
            .executor()
            .execute(() -> connectFuture.completeExceptionally(f.cause()));
        }
      });

    return handleProxyConnect(clientConfiguration, connectFuture);
  }

  private SslContext getSslContext(ImapClientConfiguration clientConfiguration) {
    if (
      !clientConfiguration.trustManagerFactory().isPresent() &&
      !clientConfiguration.allowSha1Certificates()
    ) {
      return sslContext;
    }
    try {
      TrustManagerFactory tmf = clientConfiguration
        .trustManagerFactory()
        .orElseGet(() ->
          clientConfiguration.allowSha1Certificates()
            ? buildSha1AllowingFactory(defaultTrustManagerFactory)
            : defaultTrustManagerFactory
        );

      SslContextBuilder sslContextBuilder = SslContextBuilder
        .forClient()
        .trustManager(tmf);

      if (!clientConfiguration.sslProtocols().isEmpty()) {
        sslContextBuilder.protocols(clientConfiguration.sslProtocols());
      }

      return sslContextBuilder.build();
    } catch (SSLException e) {
      throw new RuntimeException(e);
    }
  }

  private static TrustManagerFactory buildSha1AllowingFactory(
    TrustManagerFactory baseTmf
  ) {
    X509TrustManager baseTm = findX509TrustManager(baseTmf);
    X509Certificate[] acceptedIssuers = baseTm.getAcceptedIssuers();

    X509ExtendedTrustManager sha1AllowingTm = new X509ExtendedTrustManager() {
      @Override
      public void checkClientTrusted(
        X509Certificate[] chain,
        String authType,
        Socket socket
      ) throws CertificateException {
        baseTm.checkClientTrusted(chain, authType);
      }

      @Override
      public void checkClientTrusted(
        X509Certificate[] chain,
        String authType,
        SSLEngine engine
      ) throws CertificateException {
        baseTm.checkClientTrusted(chain, authType);
      }

      @Override
      public void checkServerTrusted(
        X509Certificate[] chain,
        String authType,
        Socket socket
      ) throws CertificateException {
        checkServerTrusted(chain, authType);
      }

      @Override
      public void checkServerTrusted(
        X509Certificate[] chain,
        String authType,
        SSLEngine engine
      ) throws CertificateException {
        checkServerTrusted(chain, authType);
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        baseTm.checkClientTrusted(chain, authType);
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        validateWithSha1Allowed(chain, acceptedIssuers);
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return baseTm.getAcceptedIssuers();
      }
    };

    return new TrustManagerFactory(
      new TrustManagerFactorySpi() {
        @Override
        protected void engineInit(KeyStore ks) throws KeyStoreException {}

        @Override
        protected void engineInit(ManagerFactoryParameters spec)
          throws InvalidAlgorithmParameterException {}

        @Override
        protected TrustManager[] engineGetTrustManagers() {
          return new TrustManager[] { sha1AllowingTm };
        }
      },
      null,
      TrustManagerFactory.getDefaultAlgorithm()
    ) {};
  }

  private static X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
    return Arrays
      .stream(tmf.getTrustManagers())
      .filter(X509TrustManager.class::isInstance)
      .map(X509TrustManager.class::cast)
      .findFirst()
      .orElseThrow(() ->
        new IllegalStateException("No X509TrustManager found in TrustManagerFactory")
      );
  }

  private static void validateWithSha1Allowed(
    X509Certificate[] chain,
    X509Certificate[] acceptedIssuers
  ) throws CertificateException {
    if (chain == null || chain.length == 0) {
      throw new CertificateException("Empty certificate chain");
    }

    for (X509Certificate cert : chain) {
      cert.checkValidity();
    }

    List<X509Certificate> trustedList = Arrays.asList(acceptedIssuers);

    for (int i = 0; i < chain.length; i++) {
      X509Certificate cert = chain[i];
      X500Principal issuerPrincipal = cert.getIssuerX500Principal();

      // Check if any trusted CA directly issued this cert
      for (X509Certificate trusted : acceptedIssuers) {
        if (!issuerPrincipal.equals(trusted.getSubjectX500Principal())) {
          continue;
        }
        try {
          cert.verify(trusted.getPublicKey());
          return;
        } catch (GeneralSecurityException ignored) {}
      }

      // Check if this is a self-signed trusted root
      if (
        cert.getSubjectX500Principal().equals(issuerPrincipal) &&
        trustedList.contains(cert)
      ) {
        return;
      }

      // Verify signature against the next cert in the provided chain
      if (i + 1 < chain.length) {
        X509Certificate issuerCert = chain[i + 1];
        if (!issuerPrincipal.equals(issuerCert.getSubjectX500Principal())) {
          throw new CertificateException(
            "Certificate chain broken: issuer mismatch at index " + i
          );
        }
        try {
          cert.verify(issuerCert.getPublicKey());
        } catch (GeneralSecurityException e) {
          throw new CertificateException(
            "Certificate signature verification failed at index " +
            i +
            ": " +
            e.getMessage(),
            e
          );
        }
      } else {
        throw new CertificateException(
          "Certificate chain is not anchored to a trusted certificate authority"
        );
      }
    }

    throw new CertificateException(
      "Certificate chain is not anchored to a trusted certificate authority"
    );
  }

  private CompletableFuture<ImapClient> handleProxyConnect(
    ImapClientConfiguration clientConfiguration,
    CompletableFuture<ImapClient> connectFuture
  ) {
    if (!clientConfiguration.proxyConfig().isPresent()) {
      return connectFuture;
    }
    ProxyCommand proxyCommand = new ProxyCommand(
      clientConfiguration.hostAndPort(),
      clientConfiguration.proxyConfig().get().proxyLocalIpAddress()
    );
    return connectFuture.thenCompose(imapClient ->
      imapClient
        .send(proxyCommand)
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
