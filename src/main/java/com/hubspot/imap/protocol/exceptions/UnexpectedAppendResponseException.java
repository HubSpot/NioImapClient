package com.hubspot.imap.protocol.exceptions;

import com.hubspot.imap.protocol.response.ImapResponse;

public class UnexpectedAppendResponseException extends RuntimeException {
  private static final String MESSAGE = "Received unexpected response from APPEND request: %s";

  public UnexpectedAppendResponseException() {
    super(MESSAGE);
  }

  public UnexpectedAppendResponseException(ImapResponse response) {
     super(String.format(MESSAGE, response.getMessage()));
  }

  public UnexpectedAppendResponseException(Throwable throwable) {
    super(String.format(MESSAGE, throwable.getMessage()), throwable);
  }
}
