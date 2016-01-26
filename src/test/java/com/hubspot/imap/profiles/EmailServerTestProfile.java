package com.hubspot.imap.profiles;

import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.ConnectionClosedException;
import java.util.concurrent.ExecutionException;

public abstract class EmailServerTestProfile {
  public abstract ImapClientFactory getClientFactory();
  public abstract EmailServerImplDetails getImplDetails();

  public abstract String getUsername();
  public abstract String getPassword();

  public abstract String description();

  public ImapClient getClient() throws InterruptedException {
    return getClientFactory().connect(getUsername(), getPassword());
  }

  public ImapClient getLoggedInClient() throws InterruptedException, ExecutionException, ConnectionClosedException {
    ImapClient client = getClient();
    client.login();
    client.awaitLogin();

    return client;
  }

  @Override
  public String toString() {
    return description();
  }
}
