package com.hubspot.imap.imap.command.fetch.items;

public interface FetchDataItem {
  String toString();

  enum FetchDataItemType implements FetchDataItem {
    ALL,
    FAST,
    FULL,
    FLAGS,
    INTERNALDATE,
    ENVELOPE,
    BODY,
    BODY_PEEK,
    BODYSTRUCTURE,
    RFC822,
    RFC822_HEADER,
    RFC822_SIZE,
    RFC822_TEXT,
    UID;

    public String string;

    FetchDataItemType() {
      string = name().replace("_", ".");
    }

    public String toString() {
      return string;
    }
  }
}
