package com.hubspot.imap.protocol.command;

public enum ImapCommandType {
  LOGIN,
  LOGOUT,
  NOOP,
  EXPUNGE,
  IDLE,
  AUTHENTICATE,
  LIST,
  EXAMINE,
  SELECT,
  FETCH,
  BLANK,
  STORE,
  SEARCH,
  COPY;
}
