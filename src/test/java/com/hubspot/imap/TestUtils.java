package com.hubspot.imap;

import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.imap.exceptions.ConnectionClosedException;
import com.hubspot.imap.utils.GmailUtils;

import java.util.concurrent.ExecutionException;

public class TestUtils {

  public static String USER_NAME = "hsimaptest1@gmail.com";
  public static String PASSWORD = "***REMOVED***";

  public static String ALL_MAIL = "[Gmail]/All Mail";

  public static final ImapClientFactory CLIENT_FACTORY = new ImapClientFactory(
      new ImapConfiguration.Builder()
          .setAuthType(AuthType.PASSWORD)
          .setHostAndPort(GmailUtils.GMAIL_HOST_PORT)
          .setNoopKeepAliveIntervalSec(10)
          .build()
  );


  public static ImapClient getLoggedInClient() throws ExecutionException, InterruptedException, ConnectionClosedException {
    ImapClient client = CLIENT_FACTORY.connect(USER_NAME, PASSWORD);

    client.login();
    client.awaitLogin();

    return client;
  }
}
