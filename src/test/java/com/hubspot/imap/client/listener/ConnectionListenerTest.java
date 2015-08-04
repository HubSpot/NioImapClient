package com.hubspot.imap.client.listener;

import com.hubspot.imap.TestUtils;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.client.listener.ConnectionListener.ConnectionListenerAdapter;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionListenerTest {
  @Test
  public void testOnConnect_doesCallConnect() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    try (ImapClient client = TestUtils.getLoggedInClient()) {
      client.getState().addConnectionListener(new ConnectionListenerAdapter() {
        @Override
        public void connected() {
          latch.countDown();
        }

        @Override
        public void disconnected() {}
      });

      assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  public void testOnClose_doesCallDisconnect() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    try (ImapClient client = TestUtils.getLoggedInClient()) {
      client.getState().addConnectionListener(new ConnectionListenerAdapter() {
        @Override
        public void connected() {}

        @Override
        public void disconnected() {
          latch.countDown();
        }
      });
    }

    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
  }
}
