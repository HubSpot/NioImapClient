package com.hubspot.imap.profiles;

import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.ConnectionClosedException;
import java.util.concurrent.ExecutionException;

public interface EmailServerTestProfile {
  ImapClientFactory getClientFactory();
  EmailServerImplDetails getImplDetails();

  String getUsername();
  String getPassword();

  default ImapClient getClient() throws InterruptedException {
    return getClientFactory().connect(getUsername(), getPassword());
  }

  default ImapClient getLoggedInClient() throws InterruptedException, ExecutionException, ConnectionClosedException {
    ImapClient client = getClient();
    client.login();
    client.awaitLogin();

    return client;
  }
}
