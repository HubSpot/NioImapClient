package com.hubspot.imap;

import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.profiles.EmailServerTestProfile;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import io.netty.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class MessageAddListenerTest extends ImapMultiServerTest {

  @Parameter public EmailServerTestProfile testProfile;
  private ImapClient client;

  @Before
  public void getClient() throws Exception {
    client = testProfile.getLoggedInClient();
  }

  @After
  public void closeClient() throws Exception {
    client.close();
  }

  @Test
  public void testOnOpen_doesNotCallMessageCountListener() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    client.getState().onMessageAdd((o, n) -> countDownLatch.countDown());
    Future<OpenResponse> openFuture = client.open(testProfile.getImplDetails().getAllMailFolderName(), FolderOpenMode.READ);
    openFuture.sync();

    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  public void testOnOpen_doesUpdateMessageCount() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    client.getState().addOpenEventListener((e) -> countDownLatch.countDown());
    Future<OpenResponse> openResponseFuture = client.open(testProfile.getImplDetails().getAllMailFolderName(), FolderOpenMode.READ);
    openResponseFuture.sync();

    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(client.getState().getMessageNumber()).isEqualTo(openResponseFuture.get().getExists());
  }
}
