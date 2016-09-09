package com.hubspot.imap;

import io.netty.util.AttributeKey;

public class ImapChannelAttrs {
  public static final AttributeKey<ImapConfiguration> CONFIGURATION = AttributeKey.valueOf("IMAP_CONFIG");

  private ImapChannelAttrs() {}
}
