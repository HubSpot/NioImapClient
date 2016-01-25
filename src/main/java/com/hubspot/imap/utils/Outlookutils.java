package com.hubspot.imap.utils;

import com.google.common.net.HostAndPort;

public class OutlookUtils {

  public static final HostAndPort OUTLOOK_HOST_PORT = HostAndPort.fromParts("imap.outlook.com", 993);

  public static String quote(String in) {
    return "\"" + in + "\"";
  }
}
