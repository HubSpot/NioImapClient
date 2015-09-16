package com.hubspot.imap.protocol.message;

public class StringMessageFlag implements MessageFlag {
  public String flag;

  public StringMessageFlag(String flag) {
    this.flag = flag;
  }

  @Override
  public String getString() {
    return flag;
  }
}
