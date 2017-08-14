package com.hubspot.imap.protocol.extension.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.google.common.base.Throwables;
import com.hubspot.imap.ImapMultiServerTest;
import com.hubspot.imap.TestServerConfig;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;

@RunWith(Parameterized.class)
public class GMailFetchExtensionTest extends ImapMultiServerTest {
  @Parameter
  public TestServerConfig testServerConfig;

  @Test
  public void testGmailFetchExtensions() throws Exception {
    if (!testServerConfig.imapConfiguration().hostAndPort().getHost().contains("gmail")) {
      return;
    }

    CompletableFuture<FetchResponse> responseFuture = getLoggedInClient(testServerConfig).fetch(1, Optional.of(2L), FetchDataItemType.X_GM_MSGID, FetchDataItemType.X_GM_THRID);
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
