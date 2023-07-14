package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.spotify.futures.CompletableFutures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ConcurrentConnectionTest extends BaseGreenMailServerTest {

  private static final int NUM_CONNS = 5;

  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

  @AfterClass
  public static void cleanup() {
    EXECUTOR.shutdown();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    deliverRandomMessage();
  }

  @Test
  public void testGivenMultipleConnections_canSendConcurrentNoop() throws Exception {
    List<CompletableFuture<Void>> futures = new ArrayList<>(NUM_CONNS);
    CopyOnWriteArrayList<ImapClient> clients = new CopyOnWriteArrayList<>();
    for (int i = 0; i < NUM_CONNS; i++) {
      CompletableFuture<Void> future = CompletableFuture.supplyAsync(
        () -> {
          try {
            ImapClient client = getLoggedInClient();
            clients.add(client);

            int noops = ThreadLocalRandom.current().nextInt(5);

            for (int x = 0; x < noops; x++) {
              NoopResponse response = client.noop().get();

              assertThat(response.getCode()).isEqualTo(ResponseCode.OK);

              int delay = ThreadLocalRandom.current().nextInt(200);
              Thread.sleep(delay);
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          }

          return null;
        },
        EXECUTOR
      );

      futures.add(future);
    }

    CompletableFutures.allAsList(futures).get();
    assertThat(clients).are(new Condition<>(ImapClient::isConnected, "open"));

    for (ImapClient client : clients) {
      client.close();
    }
  }
}
