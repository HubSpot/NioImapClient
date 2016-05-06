package com.hubspot.imap.protocol.extension.gmail;

import com.google.common.collect.Sets;
import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.profiles.GmailProfile;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.extension.gmail.GMailLabel.SystemLabel;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import io.netty.util.concurrent.Future;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GMailLabelTest {
  private static final GmailProfile GMAIL_PROFILE = GmailProfile.getGmailProfile();

  @Test
  public void testCanFetchLabels() throws Exception {
    Set<SystemLabel> systemLabels = Sets.immutableEnumSet(SystemLabel.DRAFTS, SystemLabel.INBOX, SystemLabel.SENT);

    try (ImapClient client = GMAIL_PROFILE.getLoggedInClient()) {
      Future<OpenResponse> openResponseFuture = client.open(GMAIL_PROFILE.getImplDetails().getAllMailFolderName(), FolderOpenMode.WRITE);
      OpenResponse or = openResponseFuture.get();
      assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

      Future<FetchResponse> fetchResponseFuture = client.fetch(1, Optional.empty(), FetchDataItemType.X_GM_LABELS);
      FetchResponse fetchResponse = fetchResponseFuture.get();

      Optional<Set<GMailLabel>> allFetchedLabels = fetchResponse.getMessages()
        .stream()
        .map(m -> {
          try {
            return m.getGMailLabels();

          } catch (UnfetchedFieldException e) {
            throw new RuntimeException(e);
          }
        }).reduce(Sets::union);

      assertThat(allFetchedLabels).isPresent();
      assertThat(allFetchedLabels.get()).containsAll(systemLabels);
    }
  }
}
