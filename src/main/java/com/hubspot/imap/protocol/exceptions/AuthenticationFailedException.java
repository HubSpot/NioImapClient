package com.hubspot.imap.protocol.exceptions;

import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;

public class AuthenticationFailedException extends RuntimeException {

  private static final BaseEncoding B64 = BaseEncoding.base64();

  public AuthenticationFailedException(String message) {
    super(message);
  }

  public AuthenticationFailedException(String message, Throwable cause) {
    super(message, cause);
  }

  public AuthenticationFailedException(Throwable cause) {
    super(cause);
  }

  public AuthenticationFailedException(
    String message,
    Throwable cause,
    boolean enableSuppression,
    boolean writableStackTrace
  ) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public AuthenticationFailedException(String message, String extraData) {
    super(String.format("%s, Extra Data: '%s'", message, extraData));
  }

  public static AuthenticationFailedException fromContinuation(String message) {
    return new AuthenticationFailedException(
      new String(B64.decode(message.trim()), StandardCharsets.UTF_8)
    );
  }

  public static AuthenticationFailedException fromContinuation(
    String message,
    String extraData
  ) {
    return new AuthenticationFailedException(
      message,
      new String(B64.decode(extraData.trim()), StandardCharsets.UTF_8)
    );
  }
}
