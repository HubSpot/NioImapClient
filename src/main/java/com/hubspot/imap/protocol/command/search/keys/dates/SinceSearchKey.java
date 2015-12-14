package com.hubspot.imap.protocol.command.search.keys.dates;

import com.hubspot.imap.protocol.command.search.keys.BaseSearchKey;
import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;
import java.util.Date;

/**
 * SINCE(RFC3501): Messages whose internal date (disregarding time and timezone) is within or later than the specified date.
 */
public class SinceSearchKey extends BaseSearchKey {
  public SinceSearchKey(Date d) {
    super(StandardSearchKeyTypes.SINCE, d.toString());
  }
}
