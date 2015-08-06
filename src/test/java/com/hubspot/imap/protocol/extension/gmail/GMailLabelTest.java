package com.hubspot.imap.protocol.extension.gmail;

import com.hubspot.imap.TestUtils;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
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
  @Test
  public void testCanFetchGMailLabels() throws Exception {
    try (ImapClient client = TestUtils.getLoggedInClient()) {
      Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
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
}
