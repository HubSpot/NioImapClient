package com.hubspot.imap.imap.response;

import com.hubspot.imap.imap.command.CommandType;

public interface Response {
  CommandType getCommandType();
  String getTag();
  String getMessage();
  ResponseCode getResponseCode();
  RawResponse getRawResponse();
  ResponseType getResponseType();

  Response fromRawResponse(RawResponse input);

  enum ResponseType {
    TAGGED,
    UNTAGGED,
    CONTINUATION;
  }
}
