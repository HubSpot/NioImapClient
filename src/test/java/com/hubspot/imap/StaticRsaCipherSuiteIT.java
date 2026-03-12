package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.net.HostAndPort;
import com.hubspot.imap.client.ImapClient;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StaticRsaCipherSuiteIT {

  private static final int SIMULATION_PORT = 10993;
  private static final HostAndPort SIMULATION_HOST = HostAndPort.fromParts(
    "localhost",
    SIMULATION_PORT
  );

  private ImapClientFactory clientFactory;

  @Before
  public void setUp() {
    clientFactory =
      new ImapClientFactory(ImapClientFactoryConfiguration.builder().build());
  }

  @After
  public void tearDown() {
    clientFactory.close();
  }

  @Test
  public void itConnectsToRsaOnlyServerWithStaticRsaCipherSuites() throws Exception {
    ImapClientConfiguration config = ImapClientConfiguration
      .builder()
      .hostAndPort(SIMULATION_HOST)
      .useSsl(true)
      .trustManagerFactory(Optional.of(InsecureTrustManagerFactory.INSTANCE))
      .sslCipherSuites(
        Set.of(
          "TLS_RSA_WITH_AES_128_CBC_SHA",
          "TLS_RSA_WITH_AES_256_CBC_SHA",
          "TLS_RSA_WITH_AES_128_GCM_SHA256",
          "TLS_RSA_WITH_AES_256_GCM_SHA384"
        )
      )
      .connectTimeoutMillis(5000)
      .build();

    try (ImapClient client = clientFactory.connect(config).join()) {
      assertThat(client.isConnected()).isTrue();
    }
  }

  @Test
  public void itConnectsToSha1CertServerWithAllowSha1Certificates() throws Exception {
    KeyStore sha1TrustStore = buildSha1TrustStore();
    try (
      ServerSocket server = startSha1ImapServer();
      ImapClientFactory sha1Factory = new ImapClientFactory(
        ImapClientFactoryConfiguration.builder().build(),
        sha1TrustStore
      )
    ) {
      HostAndPort host = HostAndPort.fromParts("localhost", server.getLocalPort());
      ImapClientConfiguration config = ImapClientConfiguration
        .builder()
        .hostAndPort(host)
        .useSsl(true)
        .allowSha1Certificates(true)
        .connectTimeoutMillis(5000)
        .build();

      try (ImapClient client = sha1Factory.connect(config).join()) {
        assertThat(client.isConnected()).isTrue();
      }
    }
  }

  @Test
  public void itFailsConnectingToSha1ServerWithoutAllowSha1Certificates()
    throws Exception {
    try (ServerSocket server = startSha1ImapServer()) {
      HostAndPort host = HostAndPort.fromParts("localhost", server.getLocalPort());
      ImapClientConfiguration config = ImapClientConfiguration
        .builder()
        .hostAndPort(host)
        .useSsl(true)
        .connectTimeoutMillis(5000)
        .build();

      ImapClient client = clientFactory.connect(config).join();
      awaitChannelClosed(client);
      assertThat(client.isConnected()).isFalse();
    }
  }

  private static void awaitChannelClosed(ImapClient client) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (client.isConnected() && System.currentTimeMillis() < deadline) {
      Thread.sleep(50);
    }
  }

  private static ServerSocket startSha1ImapServer() throws Exception {
    KeyStore serverKs = KeyStore.getInstance("PKCS12");
    try (
      InputStream is =
        StaticRsaCipherSuiteIT.class.getResourceAsStream(
            "/sha1-simulation/sha1-server.p12"
          )
    ) {
      serverKs.load(is, "changeit".toCharArray());
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(
      KeyManagerFactory.getDefaultAlgorithm()
    );
    kmf.init(serverKs, "changeit".toCharArray());

    SSLContext serverCtx = SSLContext.getInstance("TLS");
    serverCtx.init(kmf.getKeyManagers(), null, null);

    ServerSocket serverSocket = serverCtx.getServerSocketFactory().createServerSocket(0);

    Thread acceptThread = new Thread(() -> {
      while (!serverSocket.isClosed()) {
        try {
          Socket client = serverSocket.accept();
          Thread clientThread = new Thread(() -> {
            try {
              client
                .getOutputStream()
                .write(
                  "* OK [CAPABILITY IMAP4rev1 AUTH=PLAIN] SHA1 test server\r\n".getBytes()
                );
              client.getOutputStream().flush();
              byte[] buf = new byte[4096];
              while (client.getInputStream().read(buf) != -1) {}
            } catch (IOException ignored) {} finally {
              try {
                client.close();
              } catch (IOException ignored) {}
            }
          });
          clientThread.setDaemon(true);
          clientThread.start();
        } catch (IOException e) {
          break;
        }
      }
    });
    acceptThread.setDaemon(true);
    acceptThread.start();

    return serverSocket;
  }

  private static KeyStore buildSha1TrustStore() throws Exception {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);

    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    try (
      InputStream is =
        StaticRsaCipherSuiteIT.class.getResourceAsStream(
            "/sha1-simulation/sha1-server.crt"
          )
    ) {
      Certificate cert = cf.generateCertificate(is);
      ks.setCertificateEntry("sha1-imap", cert);
    }

    return ks;
  }
}
