package com.hubspot.imap.protocol.capabilities;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;

public class Capabilities {
  private static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

  private final List<Capability> capabilities;
  private final List<AuthMechanism> authMechanisms;

  public Capabilities() {
    this(Collections.emptyList());
  }

  public Capabilities(List<Capability> capabilities) {
    this.capabilities = capabilities;
    this.authMechanisms = capabilities.stream()
        .filter(capability -> capability instanceof AuthCapability)
        .map(capability -> ((AuthCapability) capability))
        .map(authCapability -> authCapability.getMechanism())
        .collect(Collectors.toList());
  }

  public List<Capability> getCapabilities() {
    return capabilities;
  }

  public List<AuthMechanism> getAuthMechanisms() {
    return authMechanisms;
  }

  public static Capabilities parseFrom(String input) {
    List<Capability> capabilities = SPACE_SPLITTER.splitToList(input).stream()
        .map(Capabilities::parseSingle)
        .collect(Collectors.toList());

    return new Capabilities(capabilities);
  }

  private static Capability parseSingle(String capability) {
    // We need to split the mechanism out for auth capabilities
    if (capability.startsWith("AUTH")) {
      String[] parts = capability.split("=");
      if (parts.length < 2) {
        return StandardCapabilities.AUTH;
      }

      return new AuthCapability(parts[1]);
    }

    return StandardCapabilities.fromString(capability).orElse(new UnknownCapability(capability));
  }
}
