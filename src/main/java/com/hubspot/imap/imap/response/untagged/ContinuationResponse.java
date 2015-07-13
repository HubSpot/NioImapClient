package com.hubspot.imap.imap.response.untagged;

import com.hubspot.imap.imap.response.TaggedResponse;
import com.hubspot.imap.imap.response.ResponseCode;

public interface ContinuationResponse extends TaggedResponse {

  class Builder extends TaggedResponse.Builder implements ContinuationResponse {

    public ContinuationResponse.Builder fromResponse(TaggedResponse taggedResponse) {
      this.setMessage(taggedResponse.getMessage())
          .setUntagged(taggedResponse.getUntagged());

      return this;
    }

    @Override
    public String getTag() {
      return null;
    }

    @Override
    public ResponseCode getCode() {
      return ResponseCode.NONE;
    }

    @Override
    public ResponseType getType() {
      return ResponseType.CONTINUATION;
    }
  }
}
