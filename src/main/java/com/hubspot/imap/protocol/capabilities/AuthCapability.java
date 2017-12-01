package com.hubspot.imap.protocol.capabilities;

public class AuthCapability {

  private final AuthMechanism mechanism;

  public AuthCapability(String name) {
    this(AuthMechanism.fromString(name));
  }

  public AuthCapability(AuthMechanism mechanism) {
    this.mechanism = mechanism;
  }

  public AuthMechanism getMechanism() {
    return mechanism;
  }
}
