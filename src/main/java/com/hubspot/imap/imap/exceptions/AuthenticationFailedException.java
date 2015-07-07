package com.hubspot.imap.imap.exceptions;

import com.google.common.io.BaseEncoding;
import com.hubspot.java.utils.Bytes;

public class AuthenticationFailedException extends Exception {
  private static final BaseEncoding B64 = BaseEncoding.base64();

  public AuthenticationFailedException(String message) {
    super(Bytes.toString(B64.decode(message)));
  }
}
