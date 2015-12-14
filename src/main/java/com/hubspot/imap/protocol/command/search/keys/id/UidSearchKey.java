package com.hubspot.imap.protocol.command.search.keys.id;

import com.hubspot.imap.protocol.command.search.keys.BaseSearchKey;
import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;

public class UidSearchKey extends BaseSearchKey{
  public UidSearchKey(String... args) {
    super(StandardSearchKeyTypes.UID, args);
  }
}
