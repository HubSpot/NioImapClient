package com.hubspot.imap;

import io.netty.util.AttributeKey;

public class ImapChannelAttrs {
  public static final AttributeKey<ImapClientFactoryConfiguration> CONFIGURATION = AttributeKey.valueOf("IMAP_CONFIG");

  private ImapChannelAttrs() {}
}
