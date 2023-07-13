package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;
import com.hubspot.imap.utils.formats.ImapDateFormat;
import java.time.ZonedDateTime;

/**
 * BEFORE (RFC3501): Messages whose internal date (disregarding time and timezone) is earlier than the specified date.
 */
public class BeforeSearchKey extends BaseSearchKey {

  public BeforeSearchKey(ZonedDateTime d) {
    super(StandardSearchKeyTypes.BEFORE, ImapDateFormat.toImapDateOnlyString(d));
  }
}
