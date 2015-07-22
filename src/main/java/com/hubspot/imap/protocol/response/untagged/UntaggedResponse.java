package com.hubspot.imap.protocol.response.untagged;

public interface UntaggedResponse {
  UntaggedResponseType getType();
  String getMessage();

  class Builder implements UntaggedResponse {
    private UntaggedResponseType type;
    private String message;

    public UntaggedResponseType getType() {
      return this.type;
    }

    public Builder setType(UntaggedResponseType type) {
      this.type = type;
      return this;
    }

    public String getMessage() {
      return this.message;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public UntaggedResponse build() {
      return this;
    }

  }
}
