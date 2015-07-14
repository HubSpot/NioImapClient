package com.hubspot.imap.imap.command;

public enum CommandType {
  LOGIN,
  LOGOUT,
  NOOP,
  IDLE,
  AUTHENTICATE,
  LIST,
  EXAMINE,
  SELECT;
}
