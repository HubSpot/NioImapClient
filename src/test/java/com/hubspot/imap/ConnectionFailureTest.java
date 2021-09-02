package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.net.HostAndPort;

public class ConnectionFailureTest extends BaseGreenMailServerTest {
  private static final int LIKELY_UNUSED_PORT_NUMBER = 10050; // sourced from https://svn.nmap.org/nmap/nmap-services

  @Test
  public void testGivenBadHost_doesThrowOnConnect() throws Exception {
    getClientFactory().connect(getImapConfig().withHostAndPort(HostAndPort.fromParts("localhost", LIKELY_UNUSED_PORT_NUMBER)))
        .handle((imapClient, throwable) -> {
          assertThat(throwable).isNotNull();
          return null;
        }).join();
  }
}
