package com.hubspot.imap;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.imap.command.fetch.FetchCommand;
import com.hubspot.imap.imap.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.imap.exceptions.AuthenticationFailedException;
import com.hubspot.imap.imap.folder.FolderMetadata;
import com.hubspot.imap.imap.message.ImapMessage;
import com.hubspot.imap.imap.message.UnfetchedFieldException;
import com.hubspot.imap.imap.response.ResponseCode;
import com.hubspot.imap.imap.response.tagged.ListResponse;
import com.hubspot.imap.imap.response.tagged.OpenResponse;
import com.hubspot.imap.imap.response.tagged.TaggedResponse;
import io.netty.util.concurrent.Future;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ImapClientTest {
  private ImapClient client;

  @Before
  public void getClient() throws Exception {
    client = TestUtils.getLoggedInClient();
  }

  @After
  public void closeClient() throws Exception {
    if (client != null && client.isLoggedIn()) {
      client.close();
    }
  }

  @Test
  public void testLogin_doesAuthenticateConnection() throws Exception {
    Future<TaggedResponse> noopResponseFuture = client.noop();
    TaggedResponse taggedResponse = noopResponseFuture.get();

    assertThat(taggedResponse.getCode()).isEqualTo(ResponseCode.OK);
  }

  @Test
  public void testList_doesReturnFolders() throws Exception {
    Future<ListResponse> listResponseFuture = client.list("", "[Gmail]/%");

    ListResponse response = listResponseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getFolders().size()).isGreaterThan(0);
    assertThat(response.getFolders()).have(new Condition<>(m -> m.getAttributes().size() > 0, "attributes"));
    assertThat(response.getFolders()).extracting(FolderMetadata::getName).contains("[Gmail]/All Mail");
  }

  @Test
  public void testGivenFolderName_canOpenFolder() throws Exception {
    Future<OpenResponse> responseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse response = responseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getExists()).isGreaterThan(0);
    assertThat(response.getFlags().size()).isGreaterThan(0);
    assertThat(response.getPermanentFlags().size()).isGreaterThan(0);
    assertThat(response.getUidValidity()).isGreaterThan(0);
    assertThat(response.getRecent()).isEqualTo(0);
    assertThat(response.getUidNext()).isGreaterThan(0);
    assertThat(response.getHighestModSeq()).isGreaterThan(0);
  }

  @Test
  public void testFetch_doesReturnMessages() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<TaggedResponse> responseFuture = client.send(new FetchCommand(1, Optional.of(5L), FetchDataItemType.FAST));
    TaggedResponse response = responseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);

    List<ImapMessage> messages = response.getUntagged().stream()
        .filter(u -> u instanceof ImapMessage).map(u -> ((ImapMessage) u))
        .collect(Collectors.toList());

    assertThat(messages).have(new Condition<>(m -> {
      try {
        return m.getSize() > 0;
      } catch (UnfetchedFieldException e) {
        return false;
      }
    }, "size"));

    assertThat(messages).have(new Condition<>(m -> {
      try {
        return m.getInternalDate().isAfter(ZonedDateTime.of(0, 1, 1, 1, 1, 1, 1, ZoneId.of("UTC")));
      } catch (UnfetchedFieldException e) {
        return false;
      }
    }, "internaldate"));
  }

  @Test
  public void testGivenInvalidCredentials_doesThrowAuthenticationException() throws Exception {
    ImapClient client = TestUtils.CLIENT_FACTORY.connect(TestUtils.USER_NAME, "");
    try {
      client.login();
      client.awaitLogin();
    } catch (ExecutionException e) {
      assertThat(e).hasCauseInstanceOf(AuthenticationFailedException.class);
    } finally {
      client.close();
    }
  }


}
