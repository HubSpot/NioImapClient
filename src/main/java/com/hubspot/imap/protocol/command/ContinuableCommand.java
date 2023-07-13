package com.hubspot.imap.protocol.command;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.response.ImapResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import java.util.concurrent.CompletableFuture;

public abstract class ContinuableCommand<T extends TaggedResponse>
  extends BaseImapCommand {

  public abstract CompletableFuture<T> continueAfterResponse(
    ImapResponse imapResponse,
    Throwable throwable
  );

  protected ImapClient imapClient;

  protected ContinuableCommand(
    ImapClient imapClient,
    ImapCommandType type,
    String... args
  ) {
    super(type, args);
    this.imapClient = imapClient;
  }
}
