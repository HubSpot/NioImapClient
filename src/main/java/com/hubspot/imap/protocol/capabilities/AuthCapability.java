package com.hubspot.imap.protocol.capabilities;

public class AuthCapability implements Capability {

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

  @Override
  public String getCapability() {
    return StandardCapabilities.AUTH + "=" + getMechanism().name();
  }
}
