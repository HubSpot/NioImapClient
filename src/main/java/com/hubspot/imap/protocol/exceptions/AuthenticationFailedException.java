package com.hubspot.imap.protocol.exceptions;

import com.google.common.io.BaseEncoding;

import java.nio.charset.StandardCharsets;

public class AuthenticationFailedException extends Exception {
  private static final BaseEncoding B64 = BaseEncoding.base64();

  public AuthenticationFailedException(String message) {
    super(message);
  }

  public static AuthenticationFailedException fromContinuation(String message) {
    return new AuthenticationFailedException(new String(B64.decode(message.trim()), StandardCharsets.UTF_8));
  }
}
