package com.hubspot.imap.protocol.capabilities;

public class UnknownCapability implements Capability {

  private final String capability;

  public UnknownCapability(String capability) {
    this.capability = capability;
  }

  @Override
  public String getCapability() {
    return capability;
  }
}
