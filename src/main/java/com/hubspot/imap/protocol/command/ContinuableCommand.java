package com.hubspot.imap.protocol.command;

import java.util.concurrent.CompletableFuture;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.response.ImapResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;

public abstract class ContinuableCommand extends BaseImapCommand {
  abstract public CompletableFuture<TaggedResponse> continueAfterResponse(ImapResponse imapResponse, Throwable throwable);
  ImapClient imapClient;

  public ContinuableCommand(ImapClient imapClient, ImapCommandType type, String... args) {
    super(type, args);
    this.imapClient = imapClient;
  }
}
