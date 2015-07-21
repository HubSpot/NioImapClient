package com.hubspot.imap.imap.exceptions;

import java.io.IOException;

public class ConnectionClosedException extends IOException {
  public ConnectionClosedException() {
  }

  public ConnectionClosedException(String message) {
    super(message);
  }
}
