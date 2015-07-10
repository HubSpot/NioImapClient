package com.hubspot.imap;

import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.imap.response.ListResponse;
import com.hubspot.imap.utils.GmailUtils;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class NioImapClient {
  public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
    ImapConfiguration configuration = new ImapConfiguration.Builder()
        .setAuthType(AuthType.XOAUTH2)
        .setHostAndPort(GmailUtils.GMAIL_HOST_PORT)
        .build();

    try (ImapClientFactory clientFactory = new ImapClientFactory(configuration)){
      ImapClient client = clientFactory.connect(args[0], args[1]);

      client.login();
      client.awaitLogin();
      client.noop();

      Future<ListResponse> future = client.list("", "[Gmail]/%");
      ListResponse response = future.get();

      while (Thread.currentThread().isAlive()) {
        Thread.sleep(500);
      }
    }
  }
}
