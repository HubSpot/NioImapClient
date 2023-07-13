package com.hubspot.imap.protocol.response.tagged;

import com.hubspot.imap.protocol.capabilities.Capabilities;
import com.hubspot.imap.protocol.response.untagged.UntaggedCapabilityResponse;

public interface CapabilityResponse extends TaggedResponse {
  Capabilities getCapabilities();

  class Builder extends TaggedResponse.Builder implements CapabilityResponse {

    private Capabilities capabilities;

    public CapabilityResponse fromResponse(TaggedResponse input) {
      capabilities =
        input
          .getUntagged()
          .stream()
          .filter(o -> o instanceof UntaggedCapabilityResponse)
          .map(o -> ((UntaggedCapabilityResponse) o))
          .map(UntaggedCapabilityResponse::getCapabilities)
          .findFirst()
          .orElse(new Capabilities());

      setCode(input.getCode());
      setMessage(input.getMessage());
      setTag(input.getTag());

      return this;
    }

    @Override
    public Capabilities getCapabilities() {
      return capabilities;
    }
  }
}
