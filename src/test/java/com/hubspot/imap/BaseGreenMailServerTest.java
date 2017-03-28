package com.hubspot.imap;

import org.junit.Before;
import org.junit.Rule;

import com.google.common.net.HostAndPort;
import com.hubspot.imap.ImapConfigurationIF.AuthType;
import com.hubspot.imap.client.ImapClient;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

public class BaseGreenMailServerTest {
  protected static final String DEFAULT_FOLDER = "INBOX";

  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

  protected GreenMailUser currentUser;

  @Before
  public void setUp() throws Exception {
    currentUser = greenMail.setUser("to@localhost.com", "to@localhost.com", "testing");
  }

  protected ImapConfiguration getImapConfig() {
    return ImapConfiguration.builder()
        .authType(AuthType.PASSWORD)
        .hostAndPort(HostAndPort.fromParts("localhost", greenMail.getImap().getPort()))
        .useSsl(false)
        .useEpoll(true)
        .connectTimeoutMillis(1000)
        .tracingEnabled(true)
        .build();
  }

  protected ImapClientFactory getClientFactory() {
    return new ImapClientFactory(getImapConfig());
  }

  protected ImapClient getLoggedInClient() throws InterruptedException {
    ImapClient client = getClientFactory().connect(currentUser.getEmail(), currentUser.getPassword());
    Thread.sleep(100);
    client.login().await();

    return client;
  }

  protected void deliverRandomMessage() {
    deliverRandomMessages(1);
  }

  protected void deliverRandomMessages(int n) {
    for (int i = 0; i < n; i++) {
      currentUser.deliver(GreenMailUtil.createTextEmail("to@localhost.com", GreenMailUtil.random() + "@localhost.com", GreenMailUtil.random(), GreenMailUtil.random(), greenMail.getImap().getServerSetup()));
    }
  }
}
