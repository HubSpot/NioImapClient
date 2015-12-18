package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.atoms.ImapAtom;

public interface SearchKey extends ImapAtom {
  String keyString();
  SearchKeyType getKeyType();
}
