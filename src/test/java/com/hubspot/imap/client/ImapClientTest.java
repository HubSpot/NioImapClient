package com.hubspot.imap.client;

import com.google.seventeen.common.base.Strings;
import com.google.seventeen.common.base.Throwables;
import com.hubspot.imap.TestUtils;
import com.hubspot.imap.protocol.command.CommandType;
import com.hubspot.imap.protocol.command.SilentStoreCommand;
import com.hubspot.imap.protocol.command.StoreCommand.StoreAction;
import com.hubspot.imap.protocol.command.fetch.UidCommand;
import com.hubspot.imap.protocol.command.fetch.items.BodyPeekFetchDataItem;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.exceptions.UnknownFetchItemTypeException;
import com.hubspot.imap.protocol.folder.FolderMetadata;
import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.MessageFlag;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.ListResponse;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.protocol.response.tagged.StreamingFetchResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import io.netty.util.concurrent.Future;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    assertThat(response.getMessages().size()).isEqualTo(5);

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
    }
  }

  @Test
  public void testFetchBodyHeaders_doesParseHeaders() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(10L), new BodyPeekFetchDataItem("HEADER"));
    FetchResponse response = responseFuture.get();
    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return !Strings.isNullOrEmpty(m.getBody().getHeader().getField("Message-ID").getBody());
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }
    }, "message id"));

    responseFuture.get();
  }

  @Test
  public void testFetchBody_doesFetchTextBody() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(2L), new BodyPeekFetchDataItem());
    FetchResponse response = responseFuture.get();
    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        if (m.getBody().isMultipart()) {
          Multipart multipart = (Multipart) m.getBody().getBody();

          for (Entity entity: multipart.getBodyParts()) {
            if (entity.getMimeType().equalsIgnoreCase("text/plain")) {
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              ((SingleBody) entity.getBody()).writeTo(baos);
              return !Strings.isNullOrEmpty(baos.toString(entity.getCharset()));
            }
          }
        }
      } catch (UnfetchedFieldException|IOException e) {
        throw Throwables.propagate(e);
      }

      return false;
    }, "text body"));

    responseFuture.get();
  }

  @Test
  public void testUidFetch() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(2L), FetchDataItemType.UID, FetchDataItemType.ENVELOPE);
    FetchResponse response = responseFuture.get();

    ImapMessage message = response.getMessages().iterator().next();

    Future<FetchResponse> uidfetchFuture = client.uidfetch(message.getUid(), Optional.of(message.getUid()), FetchDataItemType.UID, FetchDataItemType.ENVELOPE);
    FetchResponse uidresponse = uidfetchFuture.get();

    assertThat(uidresponse.getMessages().size()).isEqualTo(1);
    assertThat(uidresponse.getMessages().iterator().next().getUid()).isEqualTo(message.getUid());
  }

  @Test
  public void testStreamingFetch_doesExecuteConsumerForAllMessages() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Set<Long> uids = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    Future<StreamingFetchResponse> fetchResponseFuture = client.fetch(1, Optional.<Long>empty(), message -> {
      try {
        uids.add(message.getUid());
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }
    }, FetchDataItemType.UID);

    StreamingFetchResponse fetchResponse = fetchResponseFuture.get();

    int successful = 0;
    for (Future consumerFuture : fetchResponse.getMessageConsumerFutures()) {
      consumerFuture.get();
      successful++;
    }

    assertThat(successful).isEqualTo(uids.size());
  }

  @Test
  public void testGmailFetchExtensions() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(1, Optional.of(2L), FetchDataItemType.X_GM_MSGID, FetchDataItemType.X_GM_THRID);
    FetchResponse response = responseFuture.get();

    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return m.getGmailMessageId() > 0;
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }
    }, "gmail message id"));

    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return m.getGmailThreadId() > 0;
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }
    }, "gmail thread id"));
  }

  @Test
  public void testStore() throws Exception {
    Future<OpenResponse> openResponseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse or = openResponseFuture.get();
    assertThat(or.getCode()).isEqualTo(ResponseCode.OK);

    Future<FetchResponse> responseFuture = client.fetch(or.getExists(), Optional.<Long>empty(), FetchDataItemType.FLAGS, FetchDataItemType.UID);
    FetchResponse fetchResponse = responseFuture.get();
    ImapMessage message = fetchResponse.getMessages().iterator().next();

    TaggedResponse storeResponse = client.send(new UidCommand(
        CommandType.STORE,
        new SilentStoreCommand(StoreAction.ADD_FLAGS, message.getUid(), message.getUid(), MessageFlag.FLAGGED)
    )).get();

    assertThat(storeResponse.getCode()).isEqualTo(ResponseCode.OK);

    responseFuture = client.fetch(or.getExists(), Optional.<Long>empty(), FetchDataItemType.FLAGS, FetchDataItemType.UID);
    fetchResponse = responseFuture.get();
    ImapMessage messageWithFlagged = fetchResponse.getMessages().iterator().next();

    assertThat(messageWithFlagged.getFlags()).contains(MessageFlag.FLAGGED);

    storeResponse = client.send(new UidCommand(
        CommandType.STORE,
        new SilentStoreCommand(StoreAction.REMOVE_FLAGS, message.getUid(), message.getUid(), MessageFlag.FLAGGED)
    )).get();

    assertThat(storeResponse.getCode()).isEqualTo(ResponseCode.OK);

    responseFuture = client.fetch(or.getExists(), Optional.<Long>empty(), FetchDataItemType.FLAGS, FetchDataItemType.UID);
    fetchResponse = responseFuture.get();
    ImapMessage messageNotFlagged = fetchResponse.getMessages().iterator().next();

    assertThat(messageNotFlagged.getFlags()).isEqualTo(message.getFlags());
  }
}
