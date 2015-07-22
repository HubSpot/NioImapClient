package com.hubspot.imap;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.exceptions.UnknownFetchItemTypeException;
import com.hubspot.imap.protocol.folder.FolderMetadata;
import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.ListResponse;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import io.netty.util.concurrent.Future;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
    Future<NoopResponse> noopResponseFuture = client.noop();
    NoopResponse taggedResponse = noopResponseFuture.get();

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

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(5L), FetchDataItemType.FAST);
    FetchResponse response = responseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getMessages().size()).isGreaterThan(0);

    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return m.getSize() > 0;
      } catch (UnfetchedFieldException e) {
        return false;
      }
    }, "size"));

    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return m.getInternalDate().isAfter(ZonedDateTime.of(0, 1, 1, 1, 1, 1, 1, ZoneId.of("UTC")));
      } catch (UnfetchedFieldException e) {
        return false;
      }
    }, "internaldate"));
  }

  @Test(expected = UnfetchedFieldException.class)
  public void testAccessingUnfetchedField_doesThrowException() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(5L), FetchDataItemType.FLAGS);
    FetchResponse response = responseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getMessages().size()).isGreaterThan(0);
    response.getMessages().iterator().next().getInternalDate();
  }

  @Test
  public void testFetchUid_doesGetUid() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(5L), FetchDataItemType.UID);
    FetchResponse response = responseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);

    assertThat(response.getMessages().size()).isGreaterThan(0);
    assertThat(response.getMessages().iterator().next().getUid()).isGreaterThan(0);
  }

  @Test
  public void testFetchEnvelope_doesFetchEnvelope() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(10L), FetchDataItemType.ENVELOPE);
    FetchResponse response = responseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);

    assertThat(response.getMessages().size()).isGreaterThan(0);

    Envelope envelope = response.getMessages().iterator().next().getEnvelope();
    assertThat(envelope.getDate()).isNotNull();
    assertThat(envelope.getFrom().size()).isEqualTo(1);
    assertThat(envelope.getSender().size()).isEqualTo(1);
    assertThat(envelope.getTo().size()).isGreaterThanOrEqualTo(1);
    assertThat(envelope.getSubject()).isNotEmpty();
    assertThat(envelope.getMessageId()).isNotEmpty();
  }

  @Test
  public void testFetchBody_doesThrowUnknownFetchItemException() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(2L), FetchDataItemType.BODY);

    try {
      responseFuture.get();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).hasCauseInstanceOf(UnknownFetchItemTypeException.class);
    } finally {
      try {
        client.close();
      } catch (Exception e) {
        // This may also throw depending on the order in which things are parsed.
      }
    }
  }



}
