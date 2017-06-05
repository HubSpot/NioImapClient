package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.net.HostAndPort;

public class ConnectionFailureTest extends BaseGreenMailServerTest {
  @Test
  public void testGivenBadHost_doesThrowOnConnect() throws Exception {
    getClientFactory().connect(getImapConfig().withHostAndPort(HostAndPort.fromParts("localhost", 12345)))
        .handle((imapClient, throwable) -> {
          assertThat(throwable).isNotNull();
          return null;
        }).join();
  }
}
