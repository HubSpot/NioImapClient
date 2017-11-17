package com.hubspot.imap.protocol.exceptions;

public class UnexpectedAppendResponseException extends RuntimeException {
  private static final String MESSAGE = "Received unexpected response from APPEND request";

  public UnexpectedAppendResponseException() {
    super(MESSAGE);
  }

  public UnexpectedAppendResponseException(Throwable throwable) {
    super(MESSAGE, throwable);
  }
}
