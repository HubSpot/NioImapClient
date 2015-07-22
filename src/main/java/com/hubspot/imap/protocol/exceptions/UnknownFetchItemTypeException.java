package com.hubspot.imap.protocol.exceptions;

public class UnknownFetchItemTypeException extends Exception {
  private static final String MESSAGE_FORMAT = "Could not parse FETCH response containing an unknown fetch item type %s";

  public UnknownFetchItemTypeException(String type) {
    super(String.format(MESSAGE_FORMAT, type));
  }
}
