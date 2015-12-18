package com.hubspot.imap.protocol.command.fetch;

import com.hubspot.imap.protocol.command.BaseImapCommand;
import com.hubspot.imap.protocol.command.ImapCommand;
import com.hubspot.imap.protocol.command.ImapCommandType;

import java.util.List;

public class UidCommand extends BaseImapCommand {
  private final BaseImapCommand wrappedCommand;

  public UidCommand(ImapCommandType type, BaseImapCommand wrappedCommand) {
    super(type);
    this.wrappedCommand = wrappedCommand;
  }

  @Override
  public String getPrefix() {
    return String.format("UID %s", wrappedCommand.getPrefix());
  }

  @Override
  public ImapCommandType getCommandType() {
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

  public ImapCommand getWrappedCommand() {
    return wrappedCommand;
  }
}
