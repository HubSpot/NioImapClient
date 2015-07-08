package com.hubspot.imap.imap.response;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

public interface Response {
  String getTag();
  String getMessage();
  List<String> getUntagged();
  ResponseCode getCode();
  ResponseType getType();

  enum ResponseType {
    TAGGED,
    UNTAGGED,
    CONTINUATION;
  }

  class Builder implements Response {
    private String tag;
    private String message;
    private List<String> untagged = new ArrayList<>();
    private ResponseType type;
    private ResponseCode code;

    public Response build() {
      return this;
    }

    public String getTag() {
      return this.tag;
    }

    public Builder setTag(String tag) {
      this.tag = tag;
      return this;
    }

    public String getMessage() {
      return this.message;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public List<String> getUntagged() {
      return this.untagged;
    }

    public Builder addUntagged(String untagged) {
      this.untagged.add(untagged);
      return this;
    }

    public Builder setUntagged(List<String> untagged) {
      this.untagged = untagged;
      return this;
    }

    public ResponseType getType() {
      return this.type;
    }

    public Builder setType(ResponseType type) {
      this.type = type;
      return this;
    }

    public ResponseCode getCode() {
      return this.code;
    }

    public Builder setCode(ResponseCode code) {
      this.code = code;
      return this;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("tag", tag)
          .add("message", message)
          .add("untagged", untagged)
          .add("type", type)
          .add("code", code)
          .toString();
    }
  }
}
