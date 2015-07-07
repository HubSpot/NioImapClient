package com.hubspot.imap.imap.command;

public class ListCommand extends BaseCommand {
  public ListCommand(int tag, String reference, String query) {
    super(CommandType.LIST, tag, quote(reference), quote(query));
  }

  private static String quote(String in) {
    return "\"" + in + "\"";
  }
}
