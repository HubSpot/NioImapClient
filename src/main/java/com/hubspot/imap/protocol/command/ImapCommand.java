package com.hubspot.imap.protocol.command;

import com.hubspot.imap.protocol.command.atoms.ImapAtom;

public interface ImapCommand extends ImapAtom {
  String commandString();
  ImapCommandType getCommandType();
}
