package com.hubspot.imap.protocol.command;

import java.nio.charset.Charset;

public class StringLiteralCommand extends BaseImapCommand {

  private final String stringLiteral;
  private final Charset charset;
  private final int size;

  public StringLiteralCommand(String stringLiteral, Charset charset) {
    super(ImapCommandType.BLANK, false);
    this.stringLiteral = stringLiteral;
    this.charset = charset;
    this.size = commandString().getBytes(charset).length;
  }

  @Override
  public String commandString() {
    return stringLiteral;
  }

  public int getSize() {
    return size;
  }

  public Charset getCharset() {
    return charset;
  }
}
