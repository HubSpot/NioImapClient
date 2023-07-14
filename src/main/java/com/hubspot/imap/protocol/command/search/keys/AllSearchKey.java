package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;

public class AllSearchKey extends BaseSearchKey {

  public AllSearchKey() {
    super(StandardSearchKeyTypes.ALL);
  }
}
