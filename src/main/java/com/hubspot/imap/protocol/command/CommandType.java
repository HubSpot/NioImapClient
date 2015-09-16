package com.hubspot.imap.protocol.command;

public enum CommandType {
  LOGIN,
  LOGOUT,
  NOOP,
  IDLE,
  AUTHENTICATE,
  LIST,
  EXAMINE,
  SELECT,
  FETCH,
  BLANK,
  STORE,
  SEARCH;
}
