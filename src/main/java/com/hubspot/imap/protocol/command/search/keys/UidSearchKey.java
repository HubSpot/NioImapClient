package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;

/**
 * UID(RFC3501): Messages with unique identifiers corresponding to the specified unique identifier set.  Sequence set ranges are permitted.
 */
public class UidSearchKey extends BaseSearchKey{
  public UidSearchKey(String... args) {
    super(StandardSearchKeyTypes.UID, args);
  }
}
