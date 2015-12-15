package com.hubspot.imap.protocol.command.search.keys;

import com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes;

/**
 * @deprecated Use {@link SearchKeyType}
 */
@Deprecated
public interface SearchTermType {
  String keyString();
  SearchKeyType getKeyType();

  /**
   * @deprecated Use {@link com.hubspot.imap.protocol.command.search.keys.SearchKeyType.StandardSearchKeyTypes}
   */
  @Deprecated
  enum StandardSearchTermTypes implements SearchTermType {
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
    public String keyString() {
      return name();
    }

    @Override
    public SearchKeyType getKeyType() {
      return StandardSearchKeyTypes.valueOf(keyString());
    }
  }
}
