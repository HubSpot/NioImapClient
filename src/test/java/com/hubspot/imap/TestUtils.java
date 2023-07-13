package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Throwables;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TestUtils {

  public static List<Long> msgsToUids(Collection<ImapMessage> messages) {
    return messages.stream().map(TestUtils::msgToUid).collect(Collectors.toList());
  }

  public static long msgToUid(ImapMessage msg) {
    try {
      return msg.getUid();
    } catch (UnfetchedFieldException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static ZonedDateTime msgToInternalDate(ImapMessage msg) {
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
      FetchResponse response = client
        .uidfetch(
          uid,
          Optional.of(uid),
          FetchDataItemType.UID,
          FetchDataItemType.ENVELOPE,
          FetchDataItemType.INTERNALDATE
        )
        .get();
      Set<ImapMessage> messages = response.getMessages();

      assertThat(messages.size())
        .describedAs("Expected 1 message for uid %d, but received %s.", uid, messages)
        .isEqualTo(1);
      return messages.iterator().next();
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }
}
