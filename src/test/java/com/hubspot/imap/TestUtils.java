package com.hubspot.imap;

import com.google.common.base.Throwables;
import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.client.ImapClientTest;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.exceptions.ConnectionClosedException;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.utils.GmailUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

public class TestUtils {

  public static String USER_NAME = "hsimaptest1@gmail.com";
  public static String PASSWORD = "***REMOVED***";

  public static String ALL_MAIL = "[Gmail]/All Mail";

  public static final ImapClientFactory CLIENT_FACTORY = new ImapClientFactory(
      new ImapConfiguration.Builder()
          .setAuthType(AuthType.PASSWORD)
          .setHostAndPort(GmailUtils.GMAIL_HOST_PORT)
          .setNoopKeepAliveIntervalSec(10)
          .setUseEpoll(true)
          .build()
  );

  public static ImapClient getClient() throws InterruptedException {
    return CLIENT_FACTORY.connect(USER_NAME, PASSWORD);
  }

  public static ImapClient getLoggedInClient() throws ExecutionException, InterruptedException, ConnectionClosedException {
    ImapClient client = getClient();
    client.login();
    client.awaitLogin();

    return client;
  }

  public static List<Long> msgsToUids(List<ImapMessage> messages) {
    return messages.stream().map(TestUtils::msgToUid).collect(Collectors.toList());
  }

  public static long msgToUid(ImapMessage msg) {
    try {
      return msg.getUid();
    } catch (UnfetchedFieldException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static ZonedDateTime msgToInternalDate(ImapMessage msg){
    try {
      return msg.getInternalDate();
    } catch (UnfetchedFieldException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static List<ImapMessage> fetchMessages(ImapClient client, List<Long> uids) {
    return uids.stream().map(id -> fetchMessage(client, id)).collect(Collectors.toList());
  }

  public static ImapMessage fetchMessage(ImapClient client, long uid) {
    try {
      FetchResponse response = client.uidfetch(uid, Optional.of(uid), FetchDataItemType.UID, FetchDataItemType.ENVELOPE, FetchDataItemType.INTERNALDATE).get();
      Set<ImapMessage> messages = response.getMessages();

      assertThat(messages.size()).isEqualTo(1);
      return messages.iterator().next();
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }
}
