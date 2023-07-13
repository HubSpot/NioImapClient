package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;

public class KeywordSearchKey extends BaseSearchKey {

  public KeywordSearchKey(String keyword) {
    super(StandardSearchKeyTypes.KEYWORD, keyword);
  }
}
