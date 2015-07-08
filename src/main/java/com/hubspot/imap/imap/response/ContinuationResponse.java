package com.hubspot.imap.imap.response;

public interface ContinuationResponse extends Response {

  class Builder extends Response.Builder implements ContinuationResponse {

    public ContinuationResponse.Builder fromResponse(Response response) {
      this.setMessage(response.getMessage())
          .setUntagged(response.getUntagged());

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
