package com.hubspot.imap.protocol.command.search;

import com.hubspot.imap.protocol.command.search.keys.BeforeSearchKey;
import com.hubspot.imap.protocol.command.search.keys.SinceSearchKey;
import java.util.Date;

public class DateSearches {
  public static SearchCommand searchBefore(Date d) {
    return new SearchCommand(new BeforeSearchKey(d));
  }

  public static SearchCommand searchAfter(Date d) {
    return new SearchCommand(new SinceSearchKey(d));
  }

  public static SearchCommand searchBetween(Date start, Date end) {
    return new SearchCommand(new SinceSearchKey(start),
                             new BeforeSearchKey(end));
  }
}
