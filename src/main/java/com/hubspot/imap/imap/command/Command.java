package com.hubspot.imap.imap.command;

public interface Command {
  String commandString();
  String getId();
}
