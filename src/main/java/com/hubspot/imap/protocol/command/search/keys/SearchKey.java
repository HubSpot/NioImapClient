package com.hubspot.imap.protocol.command.search.keys;

public interface SearchKey {
  String keyString();
  SearchKeyType getKeyType();
}
