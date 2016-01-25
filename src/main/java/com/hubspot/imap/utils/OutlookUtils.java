package com.hubspot.imap.utils;

import com.google.common.net.HostAndPort;

public class OutlookUtils {
  // Yep. There's a difference. Thanks Microsoft
  public static final HostAndPort OUTLOOK_365_IMAP_HOST_PORT = HostAndPort.fromParts("outlook.office365.com", 993);
  public static final HostAndPort OUTLOOK_HOST_IMAP_PORT = HostAndPort.fromParts("imap-mail.outlook.com", 993);
}
