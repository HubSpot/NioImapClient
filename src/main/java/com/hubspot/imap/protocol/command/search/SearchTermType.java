package com.hubspot.imap.protocol.command.search;

public interface SearchTermType {
  String toString();

  enum StandardSearchTermType implements SearchTermType {
    ALL,
    ANSWERED,
    BCC,
    BEFORE,
    BODY,
    CC,
    DELETED,
    DRAFT,
    FLAGGED,
    FROM,
    HEADER,
    KEYWORD,
    LARGER,
    NEW,
    NOT,
    OLD,
    ON,
    OR,
    RECENT,
    SEEN,
    SENTBEFORE,
    SENTON,
    SENTSINCE,
    SINCE,
    SMALLER,
    SUBJECT,
    TEXT,
    TO,
    UID,
    UNANSWERED,
    UNDELETED,
    UNDRAFT,
    UNFLAGGED,
    UNKEYWORD,
    UNSEEN;

    @Override
    public String toString() {
      return name();
    }
  }
}
