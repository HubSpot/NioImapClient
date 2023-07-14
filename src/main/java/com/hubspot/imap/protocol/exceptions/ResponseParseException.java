package com.hubspot.imap.protocol.exceptions;

public class ResponseParseException extends Exception {

  public ResponseParseException(String message) {
    super(message);
  }

  public ResponseParseException(Throwable cause) {
    super(cause);
  }
}
