package com.hubspot.imap.imap.response;

import com.hubspot.imap.imap.command.CommandType;

public interface Response {
  CommandType getCommandType();
  String getTag();
  ResponseCode getResponseCode();
  RawResponse getRawResponse();
  Response fromRawResponse(RawResponse input);
}
