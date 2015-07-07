package com.hubspot.imap.imap.response;

import com.google.common.base.Objects;
import com.hubspot.imap.imap.command.CommandType;

public class BaseResponse implements Response {
  private ResponseType responseType;
  private CommandType commandType;
  private String tag;
  private String contents;
  private ResponseCode code;
  private RawResponse rawResponse;

  public BaseResponse() {}

  @Override
  public CommandType getCommandType() {
    return commandType;
  }

  public String getTag() {
    return tag;
  }

  @Override
  public String getMessage() {
    return rawResponse.getResponseMessage();
  }

  public Response fromRawResponse(RawResponse input) {
    this.responseType = input.getType();
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

  @Override
  public ResponseType getResponseType() {
    return responseType;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("responseType", responseType)
        .add("commandType", commandType)
        .add("tag", tag)
        .add("contents", contents)
        .add("code", code)
        .add("rawResponse", rawResponse)
        .toString();
  }
}
