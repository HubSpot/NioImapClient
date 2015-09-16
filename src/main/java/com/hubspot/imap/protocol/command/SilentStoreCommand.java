package com.hubspot.imap.protocol.command;

import com.hubspot.imap.protocol.message.MessageFlag;
import com.hubspot.imap.protocol.message.StandardMessageFlag;

public class SilentStoreCommand extends StoreCommand {

  public SilentStoreCommand(StoreAction action, long startId, long stopId, MessageFlag... args) {
    super(action, startId, stopId, args);
  }

  @Override
  protected String getAction() {
    return super.getAction() + ".SILENT";
  }
}
