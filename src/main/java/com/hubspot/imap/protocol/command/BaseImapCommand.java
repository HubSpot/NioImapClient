package com.hubspot.imap.protocol.command;

import java.util.Collection;
import java.util.stream.Collectors;

import com.hubspot.imap.protocol.command.atoms.BaseImapAtom;
import com.hubspot.imap.protocol.message.MessageFlag;

public class BaseImapCommand extends BaseImapAtom implements ImapCommand {
  protected final ImapCommandType type;
  private final boolean requiresTag;

  public BaseImapCommand(ImapCommandType type, String... args) {
    super(args);
    requiresTag = true;
    this.type = type;
  }

  public BaseImapCommand(ImapCommandType type, boolean requiresTag, String... args) {
    super(args);
    this.type = type;
    this.requiresTag = requiresTag;
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

  public boolean getRequiresTag() {
    return requiresTag;
  }

  String getFlagString(Collection<MessageFlag> flags) {
    return "(" + SPACE_JOINER.join(flags.stream().map(MessageFlag::getString).collect(Collectors.toList())) + ")";
  }
}
