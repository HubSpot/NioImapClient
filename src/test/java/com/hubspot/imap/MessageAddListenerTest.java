package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;

public class MessageAddListenerTest extends BaseGreenMailServerTest {

  private ImapClient client;
  private ExecutorService executorService;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    deliverRandomMessage();
    client = getLoggedInClient();
    executorService = Executors.newSingleThreadExecutor();
  }

  @After
  public void closeClient() throws Exception {
    client.close();
  }

  @Test
  public void testOnKeepAlive_doesCallMessageListener() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    client.getState().onMessageAdd((o, n) -> countDownLatch.countDown(), executorService);

    CompletableFuture<OpenResponse> openFuture = client.open(DEFAULT_FOLDER, FolderOpenMode.READ);
    openFuture.get(30, TimeUnit.SECONDS);

    deliverRandomMessage();
    assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testOnOpen_doesNotCallMessageCountListener() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    client.getState().onMessageAdd((o, n) -> countDownLatch.countDown(), executorService);
    client.list("", "%");
    CompletableFuture<OpenResponse> openFuture = client.open(DEFAULT_FOLDER, FolderOpenMode.READ);
    openFuture.get(30, TimeUnit.SECONDS);

    assertThat(openFuture.isDone()).isTrue();
    assertThat(openFuture.isCompletedExceptionally()).isFalse();
    assertThat(openFuture.get().getCode()).isEqualTo(ResponseCode.OK);
    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  public void testOnOpen_doesUpdateMessageCount() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    client.getState().addOpenEventListener((e) -> countDownLatch.countDown(), executorService);
    CompletableFuture<OpenResponse> openFuture = client.open(DEFAULT_FOLDER, FolderOpenMode.READ);
    openFuture.get(30, TimeUnit.SECONDS);

    assertThat(openFuture.isDone()).isTrue();
    assertThat(openFuture.isCompletedExceptionally()).isFalse();
    assertThat(openFuture.get().getCode()).isEqualTo(ResponseCode.OK);
    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(client.getState().getMessageNumber()).isEqualTo(openFuture.get().getExists());
  }
}
