package com.hubspot.imap.protocol.command.search;

import com.hubspot.imap.protocol.command.search.keys.BeforeSearchKey;
import com.hubspot.imap.protocol.command.search.keys.SinceSearchKey;
import java.time.ZonedDateTime;

public class DateSearches {

  /**
   * Caveat: SEARCH BEFORE uses only date (no time) - you may get some surprising results on the boundaries
   * (which are exclusive)
   */
  public static SearchCommand searchBefore(ZonedDateTime d) {
    return new SearchCommand(new BeforeSearchKey(d));
  }

  /**
   * Caveat: SEARCH AFTER uses only date (no time) - you may get some surprising results on the boundaries
   * (which are exclusive)
   */
  public static SearchCommand searchAfter(ZonedDateTime d) {
    return new SearchCommand(new SinceSearchKey(d));
  }

  /**
   * Caveat: SEARCH BEFORE x AFTER y uses only date (no time) - you may get some surprising results on the boundaries
   * (which are exclusive)
   */
  public static SearchCommand searchBetween(ZonedDateTime start, ZonedDateTime end) {
    return new SearchCommand(new SinceSearchKey(start), new BeforeSearchKey(end));
  }
}
