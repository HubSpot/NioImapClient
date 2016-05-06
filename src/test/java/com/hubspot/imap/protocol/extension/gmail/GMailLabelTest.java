package com.hubspot.imap.protocol.extension.gmail;

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
import org.assertj.core.api.Condition;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class GMailLabelTest {
  private static final GmailProfile GMAIL_PROFILE = GmailProfile.getGmailProfile();
  @Test
  public void testCanFetchGMailLabels() throws Exception {
    try (ImapClient client = GMAIL_PROFILE.getLoggedInClient()) {
      Future<OpenResponse> openResponseFuture = client.open(GMAIL_PROFILE.getImplDetails().getAllMailFolderName(), FolderOpenMode.WRITE);
      OpenResponse or = openResponseFuture.get();
      assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

      Future<FetchResponse> fetchResponseFuture = client.fetch(1, Optional.of(2L), FetchDataItemType.X_GM_LABELS);
      FetchResponse fetchResponse = fetchResponseFuture.get();

      assertThat(fetchResponse.getMessages().size()).isGreaterThan(0);
      assertThat(fetchResponse.getMessages()).have(new Condition<>(m -> {
        try {
          return m.getGMailLabels() != null;
        } catch (UnfetchedFieldException e) {
          return false;
        }
      }, "gmail labels"));
    }
  }

  @Test
  public void testCanFetchDraftLabels() throws Exception {
    try (ImapClient client = GMAIL_PROFILE.getLoggedInClient()) {
      Future<OpenResponse> openResponseFuture = client.open(GMAIL_PROFILE.getImplDetails().getAllMailFolderName(), FolderOpenMode.WRITE);
      OpenResponse or = openResponseFuture.get();
      assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

      Future<FetchResponse> fetchResponseFuture = client.fetch(1, Optional.empty(), FetchDataItemType.X_GM_LABELS);
      FetchResponse fetchResponse = fetchResponseFuture.get();

      assertThat(fetchResponse.getMessages().size()).isGreaterThan(0);
      assertThat(fetchResponse.getMessages()).haveAtLeastOne(new Condition<>(m -> {
        try {
          return m.getGMailLabels() != null && m.getGMailLabels().contains((GMailLabel) SystemLabel.DRAFTS);
        } catch (UnfetchedFieldException e) {
          return false;
        }
      }, "gmail labels"));
    }
  }
}
