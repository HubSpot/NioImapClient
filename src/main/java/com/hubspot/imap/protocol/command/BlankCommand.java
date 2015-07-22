package com.hubspot.imap.protocol.command;

public class BlankCommand implements Command {
  public static final BlankCommand INSTANCE = new BlankCommand();

  @Override
  public String commandString() {
    return "";
  }

  @Override
  public CommandType getCommandType() {
    return CommandType.BLANK;
  }
}
