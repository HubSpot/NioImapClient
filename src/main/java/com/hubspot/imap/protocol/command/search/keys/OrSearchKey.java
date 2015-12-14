package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;

/**
 * OR(RFC3501): Messages that match either search key.
 */
public class OrSearchKey extends BaseSearchKey {
  public OrSearchKey(SearchKeyType k1, SearchKeyType k2) {
    super(StandardSearchKeyTypes.OR, k1.keyString(), k2.keyString());
  }
}
