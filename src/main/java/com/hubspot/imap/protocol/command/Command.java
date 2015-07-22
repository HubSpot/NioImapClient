package com.hubspot.imap.protocol.command;

public interface Command {
  String commandString();
  CommandType getCommandType();
  //boolean needsTag();
}
