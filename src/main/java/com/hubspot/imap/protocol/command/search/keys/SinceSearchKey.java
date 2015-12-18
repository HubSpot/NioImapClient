package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;
import com.hubspot.imap.utils.formats.ImapDateFormat;
import java.time.ZonedDateTime;

/**
 * SINCE(RFC3501): Messages whose internal date (disregarding time and timezone) is within or later than the specified date.
 */
public class SinceSearchKey extends BaseSearchKey {
  public SinceSearchKey(ZonedDateTime date) {
    super(StandardSearchKeyTypes.SINCE, ImapDateFormat.toImapDateOnlyString(date));
  }
}
