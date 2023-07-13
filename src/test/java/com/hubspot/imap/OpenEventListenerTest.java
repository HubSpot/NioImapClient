package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpenEventListenerTest extends BaseGreenMailServerTest {

  private ImapClient client;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    deliverRandomMessage();
    client = getLoggedInClient();
  }

  @After
  public void closeClient() throws Exception {
    client.close();
  }

  @Test
  public void testOnOpen_doesCallOpenListener() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    client.getState().addOpenEventListener(e -> countDownLatch.countDown());
    CompletableFuture<OpenResponse> openFuture = client.open(
      DEFAULT_FOLDER,
      FolderOpenMode.READ
    );
    openFuture.join();
    assertThat(openFuture.isDone()).isTrue();
    assertThat(openFuture.isCompletedExceptionally()).isFalse();
    assertThat(openFuture.get().getCode()).isEqualTo(ResponseCode.OK);
    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
  }
}
