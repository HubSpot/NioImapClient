package com.hubspot.imap;

import com.google.common.net.HostAndPort;
import com.hubspot.imap.client.ImapClient;
import com.icegreen.greenmail.junit4.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Before;
import org.junit.Rule;

public class BaseGreenMailServerTest {

  protected static final String DEFAULT_FOLDER = "INBOX";

  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(
    new ServerSetup[] {
      new ServerSetup(ThreadLocalRandom.current().nextInt(10000, 20000), null, "imap"),
      new ServerSetup(ThreadLocalRandom.current().nextInt(10000, 20000), null, "imaps"),
    }
  );

  protected GreenMailUser currentUser;

  @Before
  public void setUp() throws Exception {
    currentUser = greenMail.setUser("to@localhost.com", "to@localhost.com", "testing");
  }

  protected ImapClientConfiguration getImapConfig() {
    return ImapClientConfiguration
      .builder()
      .hostAndPort(HostAndPort.fromParts("localhost", greenMail.getImaps().getPort()))
      .useSsl(true)
      .trustManagerFactory(Optional.of(InsecureTrustManagerFactory.INSTANCE))
      .connectTimeoutMillis(1000)
      .tracingEnabled(true)
      .noopKeepAliveIntervalSec(1)
      .build();
  }

  protected ImapClientFactory getClientFactory() {
    return new ImapClientFactory(ImapClientFactoryConfiguration.builder().build());
  }

  protected ImapClient getLoggedInClient(ImapClientConfiguration clientConfiguration) {
    ImapClient client = getClientFactory().connect(clientConfiguration).join();
    client.login(currentUser.getEmail(), currentUser.getPassword()).join();
    return client;
  }

  protected ImapClient getLoggedInClient() {
    return getLoggedInClient(getImapConfig());
  }

  protected void deliverRandomMessage() {
    deliverRandomMessages(1);
  }

  protected void deliverRandomMessages(int n) {
    for (int i = 0; i < n; i++) {
      currentUser.deliver(
        GreenMailUtil.createTextEmail(
          "to@localhost.com",
          GreenMailUtil.random() + "@localhost.com",
          GreenMailUtil.random(),
          GreenMailUtil.random(),
          greenMail.getImaps().getServerSetup()
        )
      );
    }
  }
}
