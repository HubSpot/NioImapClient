package com.hubspot.imap.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MessageImpl;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.hubspot.imap.BaseGreenMailServerTest;
import com.hubspot.imap.ImapClientConfiguration;
import com.hubspot.imap.ProxyConfig;
import com.hubspot.imap.TestUtils;
import com.hubspot.imap.protocol.capabilities.Capabilities;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.SilentStoreCommand;
import com.hubspot.imap.protocol.command.StoreCommand.StoreAction;
import com.hubspot.imap.protocol.command.fetch.UidCommand;
import com.hubspot.imap.protocol.command.fetch.items.BodyPeekFetchDataItem;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.command.search.DateSearches;
import com.hubspot.imap.protocol.command.search.keys.UidSearchKey;
import com.hubspot.imap.protocol.exceptions.UnknownFetchItemTypeException;
import com.hubspot.imap.protocol.folder.FolderMetadata;
import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.MessageFlag;
import com.hubspot.imap.protocol.message.StandardMessageFlag;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.ListResponse;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.protocol.response.tagged.SearchResponse;
import com.hubspot.imap.protocol.response.tagged.StreamingFetchResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;

public class ImapClientTest extends BaseGreenMailServerTest {

  private ImapClient client;
  private OpenResponse openResponse;

  @After
  public void cleanup() throws Exception {
    client.close();
  }

  @Before
  public void initialize() throws Exception {
    super.setUp();
    deliverRandomMessage();
    client = getLoggedInClient();
    openResponse = client.open(DEFAULT_FOLDER, FolderOpenMode.WRITE).get();
  }

  @Test
  public void testLogin_doesAuthenticateConnection() throws Exception {
    NoopResponse taggedResponse = client.noop().get();

    assertThat(taggedResponse.getCode()).isEqualTo(ResponseCode.OK);
  }

  @Test
  public void testCapability_doesSetCapabilities() throws Exception {
    Capabilities capabilities = client.capability().join();

    assertThat(capabilities.getCapabilities().size()).isGreaterThan(0);
  }

  @Test
  public void testList_doesReturnFolders() throws Exception {
    ListResponse response = client.list("", "*").get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getFolders().size()).isGreaterThan(0);
//    assertThat(response.getFolders()).have(new Condition<>(m -> m.getAttributes().size() > 0, "attributes"));
    assertThat(response.getFolders()).extracting(FolderMetadata::getName)
                                     .contains(DEFAULT_FOLDER);
  }

  @Test
  public void testGivenFolderName_canOpenFolder() throws Exception {
    assertThat(openResponse.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(openResponse.getExists()).isGreaterThan(0);
    assertThat(openResponse.getFlags().size()).isGreaterThan(0);
    assertThat(openResponse.getPermanentFlags().size()).isGreaterThan(0);
    assertThat(openResponse.getUidValidity()).isGreaterThan(0);
    assertThat(openResponse.getUidNext()).isGreaterThan(0);
  }

  @Test
  @Ignore
  public void testFetch_doesReturnMessages() throws Exception {
    deliverRandomMessages(2);

    FetchResponse response = client.fetch(1, Optional.of(2L), FetchDataItemType.FAST).get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getMessages().size()).isGreaterThan(0);
    assertThat(response.getMessages().size()).isEqualTo(2);

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
    FetchResponse response = client.fetch(1, Optional.of(2L), FetchDataItemType.FLAGS).get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getMessages().size()).isGreaterThan(0);
    response.getMessages().iterator().next().getInternalDate();
  }

  @Test
  public void testFetchUid_doesGetUid() throws Exception {
    FetchResponse response = client.fetch(1, Optional.of(2L), FetchDataItemType.UID).get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);

    assertThat(response.getMessages().size()).isGreaterThan(0);
    assertThat(response.getMessages().iterator().next().getUid()).isGreaterThan(0);
  }

  @Test
  public void testFetchEnvelope_doesFetchEnvelope() throws Exception {
    deliverRandomMessages(3);

    FetchResponse response = client.fetch(1, Optional.of(3L), FetchDataItemType.ENVELOPE).get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);

    assertThat(response.getMessages().size()).isGreaterThan(0);

    Envelope envelope = response.getMessages().iterator().next().getEnvelope();
    assertThat(envelope.getDate()).isNotNull();
    assertThat(envelope.getFrom().size()).isEqualTo(1);
    assertThat(envelope.getTo().size()).isGreaterThanOrEqualTo(1);
    assertThat(envelope.getSubject()).isNotEmpty();
    assertThat(envelope.getMessageId()).isNotEmpty();
  }

  @Test
  public void testFetchBody_doesThrowUnknownFetchItemException() throws Exception {
    try {
      client.fetch(1, Optional.of(2L), FetchDataItemType.BODY).get();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).hasCauseInstanceOf(UnknownFetchItemTypeException.class);
    }
  }

  @Test
  public void testFetchBodyHeaders_doesParseHeaders() throws Exception {
    FetchResponse response = client.fetch(1, Optional.of(10L), new BodyPeekFetchDataItem("HEADER")).get();
    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        return !Strings.isNullOrEmpty(m.getBody().getHeader().getField("Message-ID").getBody());
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }
    }, "message id"));

  }

  @Test
  public void testFetchBody_doesFetchTextBody() throws Exception {
    FetchResponse response = client.fetch(1, Optional.of(2L), new BodyPeekFetchDataItem()).get();
    assertThat(response.getMessages()).have(new Condition<>(m -> {
      try {
        SingleBody textBody = null;
        String charset = null;

        if (m.getBody().isMultipart()) {
          Multipart multipart = (Multipart) m.getBody().getBody();

          boolean hasTextBody = false;
          for (Entity entity : multipart.getBodyParts()) {
            if (entity.getMimeType().equalsIgnoreCase("text/plain")) {
              textBody = (SingleBody) entity.getBody();
              charset = entity.getCharset();
              hasTextBody = true;
            }
          }

          if (!hasTextBody) {
            return false;
          }
        } else {
          if (!(m.getBody().getBody() instanceof TextBody)) {
            return false;
          }
          textBody = (TextBody) m.getBody().getBody();
          charset = m.getBody().getCharset();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        textBody.writeTo(baos);
        return !Strings.isNullOrEmpty(baos.toString(charset));
      } catch (UnfetchedFieldException | IOException e) {
        throw Throwables.propagate(e);
      }
    }, "text body"));
  }

  @Test
  public void testUidFetch() throws Exception {
    deliverRandomMessages(5);
    FetchResponse response = client.fetch(1, Optional.empty(), FetchDataItemType.UID, FetchDataItemType.ENVELOPE).get();

    ImapMessage message = response.getMessages().iterator().next();

    CompletableFuture<FetchResponse> uidfetchFuture = client.uidfetch(message.getUid(), Optional.of(message.getUid()), FetchDataItemType.UID,
        FetchDataItemType.ENVELOPE);
    FetchResponse uidresponse = uidfetchFuture.get();

    assertThat(uidresponse.getMessages().size()).isEqualTo(1);
    assertThat(uidresponse.getMessages().iterator().next().getUid()).isEqualTo(message.getUid());
  }

  @Test
  public void testStreamingFetch_doesExecuteConsumerForAllMessages() throws Exception {
    Set<Long> uids = Collections.newSetFromMap(new ConcurrentHashMap<>());

    CompletableFuture<StreamingFetchResponse<Void>> fetchResponseFuture = client.fetch(1, Optional.empty(), message -> {
      try {
        uids.add(message.getUid());
      } catch (UnfetchedFieldException e) {
        throw Throwables.propagate(e);
      }

      return (Void) null;
    }, FetchDataItemType.UID);

    StreamingFetchResponse<Void> fetchResponse = fetchResponseFuture.get();

    int successful = 0;
    for (CompletableFuture consumerFuture : fetchResponse.getMessageConsumerFutures()) {
      consumerFuture.get();
      successful++;
    }

    assertThat(successful).isEqualTo(uids.size());
  }

  @Test
  public void testStore() throws Exception {
    CompletableFuture<FetchResponse> responseFuture = client.fetch(openResponse.getExists(), Optional.<Long>empty(),
        FetchDataItemType.FLAGS, FetchDataItemType.UID);
    FetchResponse fetchResponse = responseFuture.get();
    ImapMessage message = fetchResponse.getMessages().iterator().next();

    TaggedResponse storeResponse = client.send(new UidCommand(
        ImapCommandType.STORE,
        new SilentStoreCommand(StoreAction.ADD_FLAGS, message.getUid(), message.getUid(), StandardMessageFlag.FLAGGED)
    )).get();

    assertThat(storeResponse.getCode()).isEqualTo(ResponseCode.OK);

    responseFuture = client.fetch(openResponse.getExists(), Optional.<Long>empty(), FetchDataItemType.FLAGS,
        FetchDataItemType.UID);
    fetchResponse = responseFuture.get();
    ImapMessage messageWithFlagged = fetchResponse.getMessages().iterator().next();

    assertThat(messageWithFlagged.getFlags()).contains(StandardMessageFlag.FLAGGED);

    storeResponse = client.send(new UidCommand(
        ImapCommandType.STORE,
        new SilentStoreCommand(StoreAction.REMOVE_FLAGS, message.getUid(), message.getUid(), StandardMessageFlag.FLAGGED)
    )).get();

    assertThat(storeResponse.getCode()).isEqualTo(ResponseCode.OK);

    responseFuture = client.fetch(openResponse.getExists(), Optional.<Long>empty(), FetchDataItemType.FLAGS,
        FetchDataItemType.UID);
    fetchResponse = responseFuture.get();
    ImapMessage messageNotFlagged = fetchResponse.getMessages().iterator().next();

    assertThat(messageNotFlagged.getFlags().stream()
                                .map(MessageFlag::getString)
                                .collect(Collectors.toList()))
        .containsOnlyElementsOf(message.getFlags().stream()
                                       .map(MessageFlag::getString)
                                       .collect(Collectors.toList()));
  }

  @Test
  public void testSimpleSearch() throws Exception {
    deliverRandomMessages(3);

    // Reopen folder to get correct EXISTS
    openResponse = client.open(DEFAULT_FOLDER, FolderOpenMode.WRITE).get();
    CompletableFuture<FetchResponse> responseFuture = client.fetch(openResponse.getExists() - 2, Optional.empty(),
        FetchDataItemType.FLAGS, FetchDataItemType.UID);
    FetchResponse fetchResponse = responseFuture.get();
    ImapMessage message = fetchResponse.getMessages()
                                       .stream()
                                       .collect(Collectors.minBy(Comparator.comparing(TestUtils::msgToUid)))
                                       .get();

    SearchResponse response = client.search(
        new UidSearchKey(String.valueOf(message.getUid()) + ":" + openResponse.getUidNext())).get();
    assertThat(response.getMessageIds().size()).isEqualTo(fetchResponse.getMessages().size());

    List<Long> expectedUids = fetchResponse.getMessages().stream().map(TestUtils::msgToUid).collect(Collectors.toList());

    Set<ImapMessage> responseMessages = new HashSet<>();
    for (long messasgeId : response.getMessageIds()) {
      responseMessages.addAll(
          client.fetch(messasgeId, Optional.empty(), FetchDataItemType.FLAGS, FetchDataItemType.UID).get().getMessages());
    }

    assertThat(responseMessages.stream()
                               .map(TestUtils::msgToUid)
                               .collect(Collectors.toList()))
        .containsOnlyElementsOf(expectedUids);
  }

  @Test
  @Ignore
  public void testSearchBefore_returnsAllEmailsBeforeDate() throws Exception {
    greenMail.purgeEmailFromAllMailboxes();
    deliverRandomMessages(2);
    ZonedDateTime before = Instant.now().atZone(ZoneId.systemDefault());

    Thread.sleep(100);
    deliverRandomMessages(1);

    SearchResponse searchResponse = client.uidsearch(DateSearches.searchBefore(before)).get();
    assertThat(searchResponse.getCode()).isEqualTo(ResponseCode.OK);

    assertThat(searchResponse.getMessageIds().size()).isEqualTo(2);
  }

  @Test
  @Ignore
  public void testSearchAfter_returnsAllEmailsAfterDate() throws Exception {
    greenMail.purgeEmailFromAllMailboxes();
    deliverRandomMessages(1);
    ZonedDateTime after = Instant.now().atZone(ZoneId.systemDefault());

    Thread.sleep(100);
    deliverRandomMessages(2);

    SearchResponse searchResponse = client.uidsearch(DateSearches.searchAfter(after)).get();
    assertThat(searchResponse.getCode()).isEqualTo(ResponseCode.OK);

    assertThat(searchResponse.getMessageIds().size()).isEqualTo(2);
  }

  @Test
  @Ignore
  public void testSearchBetween_returnsAllEmailsInRange() throws Exception {
    greenMail.purgeEmailFromAllMailboxes();
    deliverRandomMessages(1);
    Thread.sleep(100);

    ZonedDateTime after = Instant.now().atZone(ZoneId.systemDefault());
    deliverRandomMessages(3);
    ZonedDateTime before = Instant.now().atZone(ZoneId.systemDefault());

    Thread.sleep(100);
    deliverRandomMessages(1);

    SearchResponse searchResponse = client.uidsearch(DateSearches.searchBetween(after, before)).get();
    assertThat(searchResponse.getCode()).isEqualTo(ResponseCode.OK);

    assertThat(searchResponse.getMessageIds().size()).isEqualTo(3);
  }

  @Test
  public void testAppend() throws Exception {
    Header header = new HeaderImpl();
    header.addField(DefaultFieldParser.parse("Subject: This is the subject"));
    header.addField(DefaultFieldParser.parse("To: hello@foo.com"));
    header.addField(DefaultFieldParser.parse("From: goodbye@foo.com"));
    header.addField(DefaultFieldParser.parse("Date: 10-MAY-1994 00:00:00 -0000 (UTC)"));
    header.addField(DefaultFieldParser.parse("Message-ID: 12345"));

    Envelope envelope = new Envelope.Builder().setDate(ZonedDateTime.of(1994, 5, 10, 0, 0, 0, 0, ZoneId.of("UTC")));

    Body body = BasicBodyFactory.INSTANCE.textBody("This is a test");

    Message message = new MessageImpl();
    message.setBody(body);
    message.setHeader(header);

    ImapMessage imapMessage = new ImapMessage.Builder()
        .setFlags(ImmutableSet.of(StandardMessageFlag.SEEN, StandardMessageFlag.RECENT))
        .setEnvelope(envelope)
        .setBody(message);

    FetchResponse preAppendFetchAll = client.fetch(1, Optional.empty(), FetchDataItemType.UID, FetchDataItemType.FLAGS, FetchDataItemType.ENVELOPE, new BodyPeekFetchDataItem()).get();
    assertThat(preAppendFetchAll.getMessages().size()).isEqualTo(1);

    TaggedResponse appendResponse = client.append(DEFAULT_FOLDER, imapMessage.getFlags(), imapMessage.getEnvelope().getDate(), imapMessage).get();
    assertThat(appendResponse.getCode()).isEqualTo(ResponseCode.OK);
    long uid = Long.parseLong(appendResponse.getMessage().substring(25, 26));

    FetchResponse postAppendFetchAll = client.fetch(1, Optional.empty(), FetchDataItemType.UID, FetchDataItemType.ENVELOPE, new BodyPeekFetchDataItem()).get();
    assertThat(postAppendFetchAll.getMessages().size()).isEqualTo(2);

    FetchResponse postAppendFetchUid = client.uidfetch(uid, Optional.of(uid), FetchDataItemType.UID, FetchDataItemType.ENVELOPE, new BodyPeekFetchDataItem()).get();
    assertThat(postAppendFetchUid.getMessages().size()).isEqualTo(1);
    assertThat(postAppendFetchUid.getMessages().iterator().next().getBody().getSubject()).isEqualToIgnoringCase("This is the subject");
    assertThat(postAppendFetchUid.getMessages().iterator().next().getEnvelope().getMessageId()).isEqualToIgnoringCase("12345");
  }



  @Test(expected = CompletionException.class)
  public void itShouldTryProxy() throws Exception {
    // we expect a read timeout because that's how the client handles RESET commands
    // TODO - if we get a RESET on the response of a command, we should return the failure that we get back.
    ImapClient client = getLoggedInClient(ImapClientConfiguration.builder()
        .hostAndPort(HostAndPort.fromParts("localhost", greenMail.getImap().getPort() + 1))
        .useSsl(false)
        .proxyConfig(Optional.of(ProxyConfig.builder()
            .proxyHost(HostAndPort.fromParts("localhost", greenMail.getImap().getPort()))
            .proxyLocalIpAddress(Optional.of("127.0.0.10"))
            .build()
        ))
        .socketTimeoutMs(500)
        .connectTimeoutMillis(500)
        .closeTimeoutSec(500)
        .tracingEnabled(true)
        .build());
  }

}
