package com.hubspot.imap.protocol.extension.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.hubspot.imap.ImapMultiServerTest;
import com.hubspot.imap.TestServerConfig;
import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.extension.gmail.GMailLabel.SystemLabel;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public class GMailLabelTest extends ImapMultiServerTest {

  @Parameter
  public TestServerConfig testServerConfig;

  @Test
  public void testCanFetchLabels() throws Exception {
    if (!testServerConfig.imapConfiguration().hostAndPort().getHost().contains("gmail")) {
      return;
    }

    Set<SystemLabel> systemLabels = Sets.immutableEnumSet(
      SystemLabel.DRAFTS,
      SystemLabel.INBOX,
      SystemLabel.SENT
    );

    try (ImapClient client = getLoggedInClient(testServerConfig)) {
      CompletableFuture<OpenResponse> openResponseFuture = client.open(
        testServerConfig.primaryFolder(),
        FolderOpenMode.WRITE
      );
      OpenResponse or = openResponseFuture.get();
      assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

      CompletableFuture<FetchResponse> fetchResponseFuture = client.fetch(
        1,
        Optional.empty(),
        FetchDataItemType.X_GM_LABELS
      );
      FetchResponse fetchResponse = fetchResponseFuture.get();

      Optional<Set<GMailLabel>> allFetchedLabels = fetchResponse
        .getMessages()
        .stream()
        .map(m -> {
          try {
            return m.getGMailLabels();
          } catch (UnfetchedFieldException e) {
            throw new RuntimeException(e);
          }
        })
        .reduce(Sets::union);

      assertThat(allFetchedLabels).isPresent();
      assertThat(allFetchedLabels.get()).containsAll(systemLabels);
    }
  }
}
