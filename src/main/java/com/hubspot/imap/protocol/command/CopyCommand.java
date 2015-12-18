package com.hubspot.imap.protocol.command;

import java.util.Optional;

public class CopyCommand extends BaseImapCommand {
  public CopyCommand(long startId, Optional<Long> stopId, String mailBoxName) {
    super(ImapCommandType.COPY, getRange(startId, stopId), mailBoxName);
  }

  private static String getRange(long startId, Optional<Long> stopId) {
    return String.format("%d:%d", startId, stopId.orElse(startId));
  }
}
