package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public class StartTlsTest extends ImapMultiServerTest {

  @Parameter
  public TestServerConfig testServerConfig;

  @Test
  public void itDoesSuccesffullyStartTls() throws Exception {
    try (ImapClient client = getClientForConfig(testServerConfig)) {
      CompletableFuture<TaggedResponse> tlsResponseFuture = client.startTls();
      tlsResponseFuture.join();
      TaggedResponse noopResponse = client.noop().join();

      assertThat(tlsResponseFuture.get().getCode()).isEqualTo(ResponseCode.OK);
      assertThat(noopResponse.getCode()).isEqualTo(ResponseCode.OK);
    }
  }
}
