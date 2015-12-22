package com.hubspot.imap;

import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.folder.FolderAttribute;
import com.hubspot.imap.protocol.folder.FolderMetadata;
import com.hubspot.imap.protocol.response.tagged.ListResponse;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.utils.GmailUtils;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class NioImapClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(NioImapClient.class);

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

      for (FolderMetadata metadata: response.getFolders()) {
        if (metadata.getAttributes().contains(FolderAttribute.ALL)) {
          Future<OpenResponse> openFuture = client.open(metadata.getName(), FolderOpenMode.READ);
          OpenResponse openResponse = openFuture.get();
          LOGGER.info("Folder opened: {}", openResponse);

          while (Thread.currentThread().isAlive()) {
            Future<NoopResponse> noopFuture = client.noop();
            NoopResponse noopResponse = noopFuture.get();

            LOGGER.info("IDLE got exists: {}", noopResponse.getExists());
            Thread.sleep(30000);
          }
        }
      }

      while (Thread.currentThread().isAlive()) {
        Thread.sleep(500);
      }
    }
  }
}
