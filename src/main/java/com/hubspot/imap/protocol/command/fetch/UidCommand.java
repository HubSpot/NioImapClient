package com.hubspot.imap.protocol.command.fetch;

import com.hubspot.imap.protocol.command.BaseCommand;
import com.hubspot.imap.protocol.command.Command;
import com.hubspot.imap.protocol.command.CommandType;

import java.util.List;

public class UidCommand extends BaseCommand {
  private final BaseCommand wrappedCommand;

  public UidCommand(CommandType type, BaseCommand wrappedCommand) {
    super(type);
    this.wrappedCommand = wrappedCommand;
  }

  @Override
  public String getCommandPrefix() {
    return String.format("UID %s", wrappedCommand.getCommandPrefix());
  }

  @Override
  public CommandType getCommandType() {
    return wrappedCommand.getCommandType();
  }

  @Override
  public List<String> getArgs() {
    return wrappedCommand.getArgs();
  }

  @Override
  public boolean hasArgs() {
    return wrappedCommand.hasArgs();
  }

  public Command getWrappedCommand() {
    return wrappedCommand;
  }
}
