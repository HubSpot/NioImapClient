package com.hubspot.imap.imap.response;

import com.google.common.base.Objects;

public interface ContinuationResponse {
  String getMessage();

  class Builder implements ContinuationResponse {
    private String message;

    public String getMessage() {
      return this.message;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public ContinuationResponse build() {
      return this;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("message", message)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Builder builder = (Builder) o;
      return Objects.equal(getMessage(), builder.getMessage());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getMessage());
    }
  }
}
