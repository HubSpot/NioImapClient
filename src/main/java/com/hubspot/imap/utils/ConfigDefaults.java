package com.hubspot.imap.utils;

import com.google.common.collect.ImmutableList;
import com.hubspot.imap.protocol.capabilities.AuthMechanism;
import java.util.List;

public class ConfigDefaults {

  private ConfigDefaults() {}

  public static final List<AuthMechanism> DEFAULT_ALLOWED_AUTH_MECHANISMS = ImmutableList.of(
    AuthMechanism.XOAUTH2,
    AuthMechanism.PLAIN,
    AuthMechanism.LOGIN
  );
}
