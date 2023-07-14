package com.hubspot.imap.protocol.response.untagged;

public interface UntaggedIntResponse {
  UntaggedResponseType getType();
  long getValue();

  class Builder implements UntaggedIntResponse {

    private UntaggedResponseType type;
    private long value;

    public long getValue() {
      return this.value;
    }

    public Builder setValue(long value) {
      this.value = value;
      return this;
    }

    public UntaggedResponseType getType() {
      return this.type;
    }

    public Builder setType(UntaggedResponseType type) {
      this.type = type;
      return this;
    }

    public UntaggedIntResponse build() {
      return this;
    }
  }
}
