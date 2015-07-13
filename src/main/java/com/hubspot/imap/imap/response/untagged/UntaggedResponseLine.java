package com.hubspot.imap.imap.response.untagged;

import com.hubspot.imap.utils.parsers.UntaggedResponseType;

public interface UntaggedResponseLine {
  UntaggedResponseType getResponseType();
  String getValue();

  class Builder implements UntaggedResponseLine {
    private UntaggedResponseType type;
    private String value;

    public UntaggedResponseType getResponseType() {
      return this.type;
    }

    public Builder setResponseType(UntaggedResponseType type) {
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

    public UntaggedResponseLine build() {
      return this;
    }
  }
}
