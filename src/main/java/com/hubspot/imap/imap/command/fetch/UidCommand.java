package com.hubspot.imap.imap.command.fetch;

import com.google.common.collect.Lists;
import com.hubspot.imap.imap.command.BaseCommand;
import com.hubspot.imap.imap.command.Command;
import com.hubspot.imap.imap.command.CommandType;

import java.util.List;

public class UidCommand extends BaseCommand {
  private final Command wrappedCommand;

  public UidCommand(CommandType type, Command wrappedCommand) {
    super(type);
    this.wrappedCommand = wrappedCommand;
  }

  @Override
  public CommandType getCommandType() {
    return wrappedCommand.getCommandType();
  }

  @Override
  public List<String> getArgs() {
    return Lists.newArrayList(getCommandString());
  }

  public String getCommandString() {
    return String.format("UID %s", wrappedCommand.toString());
  }
}
