package com.hubspot.imap.protocol.command;

import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.hubspot.imap.ProxyConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class ProxyCommand extends BaseImapCommand {

  private static final String ANY = "any";

  public ProxyCommand(ProxyConfig proxyConfig) {
    this(proxyConfig.proxyHost(), proxyConfig.proxyLocalIpAddress());
  }

  public ProxyCommand(HostAndPort destination, Optional<String> proxyLocalIp) {
    super(ImapCommandType.PROXY, false, buildProxyArg(destination, proxyLocalIp));
  }

  private static String buildProxyArg(
    HostAndPort destination,
    Optional<String> proxyLocalIp
  ) {
    String destinationIp;
    if (InetAddresses.isInetAddress(destination.getHost())) {
      destinationIp = destination.getHost();
    } else {
      destinationIp = lookupHost(destination.getHost());
    }
    return proxyLocalIp.orElse(ANY) + ":" + destinationIp + ":" + destination.getPort();
  }

  private static String lookupHost(String host) {
    try {
      return InetAddress.getByName(host).getHostAddress();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }
}
