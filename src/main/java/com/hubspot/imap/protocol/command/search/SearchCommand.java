package com.hubspot.imap.protocol.command.search;

import com.hubspot.imap.protocol.command.BaseCommand;
import com.hubspot.imap.protocol.command.CommandType;

public class SearchCommand extends BaseCommand {
  public SearchCommand(SearchTermType type, String argument) {
    super(CommandType.SEARCH, type.toString(), argument);
  }
}
