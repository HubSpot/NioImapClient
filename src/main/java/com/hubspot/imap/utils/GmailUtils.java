package com.hubspot.imap.utils;

import com.google.common.net.HostAndPort;

public class GmailUtils {

  public static final HostAndPort GMAIL_HOST_PORT = HostAndPort.fromParts("imap.gmail.com", 993);

  public static String quote(String in) {
    return "\"" + in + "\"";
  }
}
