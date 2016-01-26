package com.hubspot.imap.protocol.extension.gmail;

import com.google.common.base.Throwables;
import com.hubspot.imap.profiles.GmailProfile;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import io.netty.util.concurrent.Future;
import org.assertj.core.api.Condition;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;


public class GMailFetchExtensionTest {
  private static final GmailProfile GMAIL_PROFILE = GmailProfile.getGmailProfile();

  @Test
  public void testGmailFetchExtensions() throws Exception {
    Future<FetchResponse> responseFuture = GmailProfile.getGmailProfile().getLoggedInClient().fetch(1, Optional.of(2L), FetchDataItemType.X_GM_MSGID, FetchDataItemType.X_GM_THRID);
    FetchResponse response = responseFuture.get();

    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return m.getGmailMessageId() > 0;
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }
    }, "gmail message id"));

    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return m.getGmailThreadId() > 0;
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }
    }, "gmail thread id"));
  }
}
