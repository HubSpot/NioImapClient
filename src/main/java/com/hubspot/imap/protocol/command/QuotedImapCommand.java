package com.hubspot.imap.protocol.command;

import java.util.List;
import java.util.stream.Collectors;

public class QuotedImapCommand extends BaseImapCommand {

  private static final String QUOTE = "\"";

  public QuotedImapCommand(ImapCommandType type, String... args) {
    super(type, args);
  }

  @Override
  public List<String> getArgs() {
    return super
      .getArgs()
      .stream()
      .map(QuotedImapCommand::quote)
      .collect(Collectors.toList());
  }

  private static String quote(String in) {
    StringBuilder result = new StringBuilder();
    result.append(QUOTE);
    result.append(in);
    result.append(QUOTE);
    return result.toString();
  }
}
