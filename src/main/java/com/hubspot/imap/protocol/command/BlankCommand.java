package com.hubspot.imap.protocol.command;

public class BlankCommand extends BaseImapCommand {
  public static final BlankCommand INSTANCE = new BlankCommand();

  public BlankCommand() {
    super(ImapCommandType.BLANK);
  }

  @Override
  public String commandString() {
    return "";
  }

  @Override
  public ImapCommandType getCommandType() {
    return ImapCommandType.BLANK;
  }
}
