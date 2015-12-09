package com.hubspot.imap.protocol.command.search;

import com.hubspot.imap.protocol.command.BaseCommand;
import com.hubspot.imap.protocol.command.CommandType;
import com.hubspot.imap.protocol.command.search.SearchTermType.StandardSearchTermType;
import java.util.Date;

public class SearchCommand extends BaseCommand {
  public SearchCommand(SearchTermType type, String argument) {
    super(CommandType.SEARCH, type.toString(), argument);
  }

  public static SearchCommand searchFrom(Date start) {
    return new SearchCommand(StandardSearchTermType.SINCE, start.toString());
  }
}
