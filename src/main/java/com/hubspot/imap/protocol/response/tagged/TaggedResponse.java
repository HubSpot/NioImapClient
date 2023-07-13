package com.hubspot.imap.protocol.response.tagged;

import com.google.common.base.MoreObjects;
import com.hubspot.imap.protocol.response.ImapResponse;
import com.hubspot.imap.protocol.response.ResponseCode;
import java.util.ArrayList;
import java.util.List;

public interface TaggedResponse extends ImapResponse {
  String getTag();
  List<Object> getUntagged();
  ResponseCode getCode();

  class Builder implements TaggedResponse {

    private String tag;
    private String message;
    private List<Object> untagged = new ArrayList<>();
    private ResponseCode code;

    public TaggedResponse build() {
      return this;
    }

    protected void copy(TaggedResponse response) {
      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());
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

    public List<Object> getUntagged() {
      return this.untagged;
    }

    public Builder addUntagged(String untagged) {
      this.untagged.add(untagged);
      return this;
    }

    public Builder setUntagged(List<Object> untagged) {
      this.untagged = untagged;
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
      return MoreObjects
        .toStringHelper(this)
        .add("tag", tag)
        .add("message", message)
        .add("untagged", untagged)
        .add("code", code)
        .toString();
    }
  }
}
