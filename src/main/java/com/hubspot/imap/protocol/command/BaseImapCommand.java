package com.hubspot.imap.protocol.command;

import com.hubspot.imap.protocol.command.atoms.BaseImapAtom;

public class BaseImapCommand extends BaseImapAtom implements ImapCommand {
  protected final ImapCommandType type;

  public BaseImapCommand(ImapCommandType type, String... args) {
    super(args);
    this.type = type;
  }

  public String commandString() {
    return imapString();
  }

  public String getPrefix() {
    return type.name();
  }

  public ImapCommandType getCommandType() {
    return type;
  }
}
