package com.hubspot.imap.protocol.command;

import java.nio.charset.Charset;

import com.google.common.base.Joiner;

public class StringLiteralCommand extends BaseImapCommand {
  protected static final Joiner SPACE_JOINER = Joiner.on(" ").skipNulls();

  public StringLiteralCommand(String... args) {
    super(ImapCommandType.BLANK, args);
  }

  @Override
  public String commandString() {
    return String.format("%s", SPACE_JOINER.join(getArgs()));
  }

  public int size(String charset) {
    return commandString().getBytes(Charset.forName(charset)).length;
  }
}
