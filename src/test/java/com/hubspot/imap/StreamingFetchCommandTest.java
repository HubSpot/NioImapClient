package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.protocol.response.tagged.StreamingFetchResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StreamingFetchCommandTest extends BaseGreenMailServerTest {

  private ImapClient client;
  private long uidNext;

  @After
  public void cleanup() throws Exception {
    client.close();
  }

  @Before
  public void initialize() throws Exception {
    super.setUp();
    client = getLoggedInClient();
    deliverRandomMessage();
    CompletableFuture<OpenResponse> openFuture = client.open(
      DEFAULT_FOLDER,
      FolderOpenMode.READ
    );
    uidNext = openFuture.join().getUidNext();
  }

  @Test
  public void testOnFetch_ItCallsBackStreamingMethod() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    CompletableFuture<StreamingFetchResponse<Void>> fetchFuture = client.uidfetch(
      uidNext,
      Optional.empty(),
      imapMessage -> {
        countDownLatch.countDown();
        return null;
      },
      FetchDataItemType.ENVELOPE
    );
    fetchFuture.join();
    assertThat(fetchFuture.isDone()).isTrue();
    assertThat(fetchFuture.isCompletedExceptionally()).isFalse();
    assertThat(fetchFuture.get().getCode()).isEqualTo(ResponseCode.OK);

    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
  }
}
