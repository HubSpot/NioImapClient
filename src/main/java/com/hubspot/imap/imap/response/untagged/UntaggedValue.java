package com.hubspot.imap.imap.response.untagged;

import com.hubspot.imap.utils.parsers.UntaggedResponseType;

public interface UntaggedValue {
  UntaggedResponseType getType();
  String getValue();

  class Builder implements UntaggedValue {
    private UntaggedResponseType type;
    private String value;

    public UntaggedResponseType getType() {
      return this.type;
    }

    public Builder setType(UntaggedResponseType type) {
      this.type = type;
      return this;
    }

    public String getValue() {
      return this.value;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public UntaggedValue build() {
      return this;
    }

  }
}
