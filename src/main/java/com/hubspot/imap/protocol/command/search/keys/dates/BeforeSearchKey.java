package com.hubspot.imap.protocol.command.search.keys.dates;

import com.hubspot.imap.protocol.command.search.keys.BaseSearchKey;
import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;
import java.util.Date;

/**
 * BEFORE (RFC3501): Messages whose internal date (disregarding time and timezone) is earlier than the specified date.
 */
public class BeforeSearchKey extends BaseSearchKey {
  public BeforeSearchKey(Date d) {
    super(StandardSearchKeyTypes.BEFORE, d.toString());
  }
}
