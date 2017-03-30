package com.hubspot.imap.protocol.command.option;

public enum ReturnOption {
  SPECIAL_USE("SPECIAL-USE"),
  ;

  String name;

  ReturnOption(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
