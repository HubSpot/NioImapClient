package com.hubspot.imap;

import java.util.Optional;

import com.google.common.net.HostAndPort;
import com.hubspot.imap.client.ImapClient;

public class BenTest {
  public static void main(String[] args) {
    ImapClientFactory factory = new ImapClientFactory();
    ImapClient client = factory.connect(ImapClientConfiguration.builder()
            .socksProxyConfig(Optional.of(SocksProxyConfig.builder()
                .proxyHost(HostAndPort.fromParts("localhost", 1080))
                .build()))
            .useSsl(true)
            .hostAndPort(HostAndPort.fromParts("ponyexpress.ponymail.net", 143))
            .build())
        .join();
    System.out.println(client);

  }
}
