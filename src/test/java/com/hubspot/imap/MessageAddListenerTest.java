package com.hubspot.imap;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.imap.response.tagged.OpenResponse;
import io.netty.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageAddListenerTest {

  private ImapClient client;

  @Before
  public void getClient() throws Exception {
    client = TestUtils.getLoggedInClient();
  }

  @After
  public void closeClient() throws Exception {
    client.close();
  }

  @Test
  public void testOnOpen_doesNotCallMessageCountListener() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    client.getState().onMessageAdd((o, n) -> countDownLatch.countDown());
    Future<OpenResponse> openFuture = client.open(TestUtils.ALL_MAIL, true);
    openFuture.sync();

    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isFalse();
  }
}
