package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.atoms.BaseImapAtom;

public class BaseSearchKey extends BaseImapAtom implements SearchKey {
  private final SearchKeyType keyType;

  public BaseSearchKey(SearchKeyType type, String... args) {
    super(args);
    this.keyType = type;
  }

  public String keyString() {
    return imapString();
  }

  public String getPrefix() {
    return keyType.keyString();
  }

  public SearchKeyType getKeyType() {
    return keyType;
  }
}
