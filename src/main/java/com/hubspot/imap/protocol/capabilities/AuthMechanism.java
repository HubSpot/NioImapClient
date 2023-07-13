package com.hubspot.imap.protocol.capabilities;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;

public enum AuthMechanism {
  LOGIN,
  PLAIN,
  XOAUTH2,
  NTLM,
  GSSAPI,
  UNKNOWN;

  private static final Map<String, AuthMechanism> INDEX = Maps.uniqueIndex(
    Arrays.asList(AuthMechanism.values()),
    authMechanism -> authMechanism.name().toLowerCase()
  );

  public static AuthMechanism fromString(String name) {
    return INDEX.getOrDefault(name.toLowerCase(), AuthMechanism.UNKNOWN);
  }
}
