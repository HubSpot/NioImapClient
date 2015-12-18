package com.hubspot.imap.protocol.extension.gmail;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType;

public enum GmailSearchKeyTypes implements SearchKeyType {
  RAW("X-GM-RAW"),
  MSGID("X-GM-MSGID"),
  THRID("X-GM-THRID");

  public final String string;

  GmailSearchKeyTypes(String string) {
    this.string = string;
  }

  @Override
  public String keyString() {
    return string;
  }
}
