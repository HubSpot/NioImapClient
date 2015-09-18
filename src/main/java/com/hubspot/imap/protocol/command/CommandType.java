package com.hubspot.imap.protocol.command;

public enum CommandType {
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
