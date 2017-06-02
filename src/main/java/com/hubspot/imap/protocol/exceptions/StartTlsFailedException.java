package com.hubspot.imap.protocol.exceptions;

public class StartTlsFailedException extends RuntimeException {
  public StartTlsFailedException(String message) {
    super(String.format("Start TLS failed. Response was: %s", message));
  }

}
