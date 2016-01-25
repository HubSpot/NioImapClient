package com.hubspot.imap;

import com.google.common.collect.Sets;
import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.profiles.EmailServerTestProfile;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class FetchEventListenerTest extends ImapMultiServerTest {

  @Parameter public EmailServerTestProfile testProfile;
  ImapClient client;

  @Before
  public void getClient() throws Exception {
    client = testProfile.getLoggedInClient();
  }

  @After
  public void closeClient() throws Exception {
    client.close();
  }

  @Test
  public void testOnFetch_doesFireFetchEvent() throws Exception {
    Set<ImapMessage> eventMessages = Sets.newHashSet();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    client.getState().addFetchEventListener((event) -> {
      eventMessages.addAll(event.getMessages());
      countDownLatch.countDown();
    });

    client.open("[Gmail]/All Mail", FolderOpenMode.READ).sync();
    FetchResponse response = client.fetch(1, Optional.<Long>empty(), FetchDataItemType.UID).get();

    countDownLatch.await();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(eventMessages.size()).isGreaterThan(0);
    assertThat(eventMessages).containsAll(response.getMessages());
  }
}
