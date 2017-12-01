package com.hubspot.imap.protocol.response.untagged;

import com.hubspot.imap.protocol.capabilities.Capabilities;

public class UntaggedCapabilityResponse implements UntaggedResponse {

  private final Capabilities capabilities;

  public UntaggedCapabilityResponse(Capabilities capabilities) {
    this.capabilities = capabilities;
  }

  @Override
  public UntaggedResponseType getType() {
    return UntaggedResponseType.CAPABILITY;
  }

  @Override
  public String getMessage() {
    return null;
  }

  public Capabilities getCapabilities() {
    return capabilities;
  }
}
