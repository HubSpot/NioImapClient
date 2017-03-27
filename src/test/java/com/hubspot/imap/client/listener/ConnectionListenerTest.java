package com.hubspot.imap.client.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.hubspot.imap.ImapMultiServerTest;
import com.hubspot.imap.TestServerConfig;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.client.listener.ConnectionListener.ConnectionListenerAdapter;

@RunWith(Parameterized.class)
public class ConnectionListenerTest extends ImapMultiServerTest {
  @Parameter public TestServerConfig testServerConfig;

  @Test
  public void testOnConnect_doesCallConnect() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    try (ImapClient client = getLoggedInClient(testServerConfig)) {
      client.getState().addConnectionListener(new ConnectionListenerAdapter() {
        @Override
        public void connected() {
          latch.countDown();
        }

        @Override
        public void disconnected() {}
      });

      assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  public void testOnClose_doesCallDisconnect() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    try (ImapClient client = getLoggedInClient(testServerConfig)) {
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
