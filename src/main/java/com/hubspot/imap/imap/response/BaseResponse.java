package com.hubspot.imap.imap.response;

import com.hubspot.imap.imap.command.CommandType;

public class BaseResponse implements Response {
  private CommandType type;
  private String tag;
  private String contents;
  private ResponseCode code;
  private RawResponse rawResponse;

  public BaseResponse() {}

  @Override
  public CommandType getCommandType() {
    return type;
  }

  public String getTag() {
    return tag;
  }

  public Response fromRawResponse(RawResponse input) {
    this.tag = input.getTag();
    this.contents = input.getResponseMessage();
    this.code = input.getResponseCode();
    this.rawResponse = input;

    return this;
  }

  @Override
  public RawResponse getRawResponse() {
    return rawResponse;
  }

  public String getContents() {
    return contents;
  }

  public ResponseCode getResponseCode() {
    return code;
  }
}
