package com.hubspot.imap.protocol.message;

public class UnfetchedFieldException extends Exception {
  private static final String MESSAGE_FORMAT = "Cannot access unfetched field \"%s\" please add it to your fetch request";

  public UnfetchedFieldException(String field) {
    super(String.format(MESSAGE_FORMAT, field));
  }
}
