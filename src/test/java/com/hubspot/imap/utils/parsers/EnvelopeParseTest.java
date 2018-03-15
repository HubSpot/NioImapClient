package com.hubspot.imap.utils.parsers;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.HeaderImpl;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapAddress;
import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.NestedArrayParser.Recycler;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import com.hubspot.imap.utils.parsers.string.LiteralStringParser;

public class EnvelopeParseTest {


  private static final byte[] TEST_ENVELOPE = "(\"Tue, 28 Jul 2015 14:03:15 +0000\" \"Declined: weekly tennis @ Tue Jul 28, 2015 7pm - 9pm (jhuang@hubspot.com)\" ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((\"Calendar <calendar-notification@google.com>\" NIL \"Google\" NIL)) ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((NIL NIL \"jhuang\" \"hubspot.com\")) NIL NIL NIL \"<001a1135b6f27860f9051befeeb2@google.com>\"))\n".getBytes(StandardCharsets.UTF_8);
  private static final byte[] TEST_ENVELOPE_EMPTY_SUBJECT = "(\"Fri, 23 Jun 2017 17:55:45 +0200\" \"\" ((\"Test Name\" NIL \"testing0478\" \"gmail.com\")) ((\"Test Again\" NIL \"test12345\" \"gmail.com\")) ((\"Test Other\" NIL \"test456\" \"gmail.com\")) ((NIL NIL \"info\" \"test.com\")) NIL NIL NIL \"<testmessageid@mail.gmail.com>\")".getBytes(StandardCharsets.UTF_8);
  private static final byte[] TEST_ENVELOPE_NIL_ADDRESS = "(\"Tue, 28 Jul 2015 14:03:15 +0000\" \"Declined: weekly tennis @ Tue Jul 28, 2015 7pm - 9pm (jhuang@hubspot.com)\" (NIL (\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((\"Calendar <calendar-notification@google.com>\" NIL \"Google\" NIL)) ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((NIL NIL \"jhuang\" \"hubspot.com\")) NIL NIL NIL \"<001a1135b6f27860f9051befeeb2@google.com>\"))\n".getBytes(StandardCharsets.UTF_8);

  private static final Mailbox ADDRESS1 = new Mailbox("bcox5021", "gmail.com");
  private static final Mailbox ADDRESS2 = new Mailbox("bcox", "hubspot.com");
  private static final Mailbox ADDRESS3 = new Mailbox("brian", "itscharlieb.com");
  private static final NestedArrayParser.Recycler<String> ARRAY_PARSER_RECYCLER = new Recycler<>(new LiteralStringParser(new SoftReferencedAppendableCharSequence(1000), 100000));

  private Header header;

  @Before
  public void setup() {
    header = new HeaderImpl();
    header.addField(Fields.bcc());
    header.addField(Fields.cc(ADDRESS2));
    header.addField(Fields.to(ADDRESS3));
    header.addField(Fields.date(Date.from(EnvelopeParser.parseDate("Mon, 29 Jan 2018 19:23:47 -0500").toInstant())));
    header.addField(Fields.from(ADDRESS1));
    header.addField(Fields.messageId("<CAKSL6jJTr_uwGqRDh4juaLLWdkmw0NyGcyiCYMFD15xm_A0=+w@mail.gmail.com>"));
    header.addField(Fields.subject("Multiple two field"));
  }

  @Test
  public void testCanParseEnvelope() throws Exception {
    NestedArrayParser<String> nestedArrayParser = ARRAY_PARSER_RECYCLER.get();
    Envelope envelope = EnvelopeParser.parse(nestedArrayParser.parse(wrappedBuffer(TEST_ENVELOPE)));
    nestedArrayParser.recycle();

    assertThat(envelope.getSubject()).isNotEmpty();
  }

  @Test
  public void testCanParseEnvelope2() throws Exception {
    NestedArrayParser<String> nestedArrayParser = ARRAY_PARSER_RECYCLER.get();
    Envelope envelope = EnvelopeParser.parse(nestedArrayParser.parse(wrappedBuffer(TEST_ENVELOPE_EMPTY_SUBJECT)));
    nestedArrayParser.recycle();

    assertThat(envelope.getSubject()).isEmpty();
    assertThat(envelope.getDateString()).isNotEmpty();
  }

  @Test
  public void testCanParseEnvelope3() throws Exception {
    NestedArrayParser<String> nestedArrayParser = ARRAY_PARSER_RECYCLER.get();
    Envelope envelope = EnvelopeParser.parse(nestedArrayParser.parse(wrappedBuffer(TEST_ENVELOPE_NIL_ADDRESS)));
    nestedArrayParser.recycle();

    assertThat(envelope.getSubject()).isNotEmpty();
  }

  @Test
  public void testCanParseHeader() throws Exception {
    Envelope envelope = EnvelopeParser.parseHeader(header);
    assertThat(envelope.getInReplyTo()).isNullOrEmpty();
    assertThat(envelope.getMessageId()).isNotEmpty();
  }

  @Test
  public void testAddressParsing() throws Exception {
    ImapAddress brianWithPersonal = new ImapAddress.Builder().setPersonal("bcox, Brian Cox").setAddress("brian@test.com");
    ImapAddress billWithPersonal = new ImapAddress.Builder().setPersonal("Cox, Bill").setAddress("bill@test.com");
    ImapAddress bobWithPersonal = new ImapAddress.Builder().setPersonal("Bob Cox").setAddress("bob@test.com");
    ImapAddress brian = new ImapAddress.Builder().setAddress("brian@test.com");
    ImapAddress bill = new ImapAddress.Builder().setAddress("bill@test.com");
    ImapAddress bob = new ImapAddress.Builder().setAddress("bob@test.com");

    List<ImapAddress> addressListWithPersonal = Lists.newArrayList(brianWithPersonal, billWithPersonal, bobWithPersonal);
    List<ImapAddress> addressList = Lists.newArrayList(brian, bill, bob);

    String addresses = "bcox, Brian Cox <brian@test.com>, Cox, Bill <bill@test.com>, Bob Cox <bob@test.com>";
    String addresses1 = "brian@test.com, bill@test.com, bob@test.com";
    String addresses2 = "<brian@test.com>, <bill@test.com>, <bob@test.com>";


    List<ImapAddress> result = EnvelopeParser.emailAddressesFromStringList(addresses, Collections.emptyList());
    List<ImapAddress> result1 = EnvelopeParser.emailAddressesFromStringList(addresses1, Collections.emptyList());
    List<ImapAddress> result2 = EnvelopeParser.emailAddressesFromStringList(addresses2, Collections.emptyList());
    assertThat(result).containsOnlyElementsOf(addressListWithPersonal);
    assertThat(result1).containsOnlyElementsOf(addressList);
    assertThat(result2).containsOnlyElementsOf(addressList);
  }
}
