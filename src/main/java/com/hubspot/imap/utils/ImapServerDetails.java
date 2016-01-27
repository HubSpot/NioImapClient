package com.hubspot.imap.utils;

import com.google.common.net.HostAndPort;

public enum ImapServerDetails {
  GMAIL(HostAndPort.fromParts("imap.gmail.com", 993)),
  OUTLOOK(HostAndPort.fromParts("imap-mail.outlook.com", 993)),
  OUTLOOK_365(HostAndPort.fromParts("outlook.office365.com", 993)),
  YAHOO(HostAndPort.fromParts("imap.mail.yahoo.com", 993));

  private HostAndPort hostAndPort;

  ImapServerDetails(HostAndPort hostAndPort) {
    this.hostAndPort = hostAndPort;
  }

  public HostAndPort hostAndPort() {
    return hostAndPort;
  }
}
