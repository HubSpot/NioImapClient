package com.hubspot.imap.protocol.command.auth;

import java.util.concurrent.CompletableFuture;

import com.google.common.io.BaseEncoding;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.ContinuableCommand;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.SimpleStringCommand;
import com.hubspot.imap.protocol.response.ImapResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import com.spotify.futures.CompletableFutures;

public class AuthenticatePlainCommand extends ContinuableCommand<TaggedResponse> {
  private static final String PLAIN = "PLAIN";
  private static final String NUL = "\0";

  private final String user;
  private final String password;

  public AuthenticatePlainCommand(ImapClient imapClient,
                                  String user,
                                  String password, String... args) {
    super(imapClient, ImapCommandType.AUTHENTICATE, PLAIN);
    this.user = user;
    this.password = password;
  }

  public CompletableFuture<TaggedResponse> continueAfterResponse(ImapResponse imapResponse, Throwable throwable) {
    if (throwable != null) {
      return CompletableFutures.exceptionallyCompletedFuture(throwable);
    }

    String toEncode = NUL + user + NUL + password;
    String base64EncodedAuthRequest = BaseEncoding.base64().encode(toEncode.getBytes());
    return imapClient.send(new SimpleStringCommand(base64EncodedAuthRequest));
  }
}
