package com.hubspot.imap.protocol.command;

import java.nio.charset.Charset;

public class StringLiteralCommand extends BaseImapCommand {
  private final String stringLiteral;

  public StringLiteralCommand(String stringLiteral) {
    super(ImapCommandType.BLANK, false);
    this.stringLiteral = stringLiteral;
  }

  @Override
  public String commandString() {
    return stringLiteral;
  }

  public int size(String charset) {
    return commandString().getBytes(Charset.forName(charset)).length;
  }
}
