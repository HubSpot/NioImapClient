package com.hubspot.imap.protocol.command.option;

public enum SelectOption {
  SPECIAL_USE("SPECIAL-USE"),
  ;

  String name;

  SelectOption(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
