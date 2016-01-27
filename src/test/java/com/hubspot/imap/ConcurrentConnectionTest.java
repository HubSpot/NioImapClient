package com.hubspot.imap;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.profiles.EmailServerTestProfile;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import io.netty.util.concurrent.Future;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ConcurrentConnectionTest extends ImapMultiServerTest {
  private static final int NUM_CONNS = 5;

  @Parameter public EmailServerTestProfile testProfile;
  private static ListeningExecutorService executorService;

  @BeforeClass
  public static void setup() {
    executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
  }

  @AfterClass
  public static void cleanup() {
    executorService.shutdown();
  }

  @Test
  public void testGivenMultipleConnections_canSendConcurrentNoop() throws Exception {
    List<ListenableFuture<Void>> futures = new ArrayList<>(NUM_CONNS);
    CopyOnWriteArrayList<ImapClient> clients = new CopyOnWriteArrayList<>();
    for (int i = 0; i < NUM_CONNS; i++) {
      ListenableFuture<Void> future = executorService.submit(() -> {
        ImapClient client = testProfile.getLoggedInClient();
        clients.add(client);

        int noops = ThreadLocalRandom.current().nextInt(5);
        for (int x = 0; x < noops; x++) {
          Future<NoopResponse> noopResponseFuture = client.noop();
          NoopResponse response = noopResponseFuture.get();

          assertThat(response.getCode()).isEqualTo(ResponseCode.OK);

          int delay = ThreadLocalRandom.current().nextInt(200);
          Thread.sleep(delay);
        }
        return null;
      });

      futures.add(future);
    }

    Futures.allAsList(futures).get();
    assertThat(clients).are(new Condition<>(ImapClient::isConnected, "open"));

    for (ImapClient client : clients) {
      client.close();
    }
  }
}
