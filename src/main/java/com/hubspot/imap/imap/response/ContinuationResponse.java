package com.hubspot.imap.imap.response;

import com.hubspot.imap.imap.command.CommandType;

public class ContinuationResponse implements Response {
  private CommandType commandType;
  private RawResponse rawResponse;
  @Override
  public CommandType getCommandType() {
    return commandType;
  }

  @Override
  public String getTag() {
    return null;
  }

  @Override
  public String getMessage() {
    return rawResponse.getResponseMessage();
  }

  @Override
  public ResponseCode getResponseCode() {
    return ResponseCode.NONE;
  }

  @Override
  public RawResponse getRawResponse() {
    return rawResponse;
  }

  @Override
  public ResponseType getResponseType() {
    return ResponseType.CONTINUATION;
  }

  @Override
  public Response fromRawResponse(RawResponse input) {
    this.rawResponse = input;
    return this;
  }
}
