package com.hubspot.imap.protocol.command;

public class SimpleStringCommand extends BaseImapCommand {
  private final String string;

  public SimpleStringCommand(String string) {
    super(ImapCommandType.BLANK, false);
    this.string = string;
  }

  @Override
  public String commandString() {
    return string;
  }
}
