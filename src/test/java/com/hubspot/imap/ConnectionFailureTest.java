package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.net.HostAndPort;

public class ConnectionFailureTest extends BaseGreenMailServerTest {
  @Override
  protected ImapConfiguration getImapConfig() {
    return super.getImapConfig().withUseSsl(true);
  }

  @Override
  protected ImapClientFactory getClientFactory() {
    return new ImapClientFactory(super.getImapConfig().withUseSsl(true)
        .withTracingEnabled(true)
        .withHostAndPort(HostAndPort.fromParts("localhost", 12345)));
  }

  @Test
  public void testGivenBadHost_doesThrowOnConnect() throws Exception {
    getClientFactory().connect()
        .handle((imapClient, throwable) -> {
          assertThat(throwable).isNotNull();
          return null;
        }).join();
  }
}
