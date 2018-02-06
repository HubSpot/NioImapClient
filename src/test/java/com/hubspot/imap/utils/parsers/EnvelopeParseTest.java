package com.hubspot.imap.utils.parsers;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.NestedArrayParser.Recycler;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import com.hubspot.imap.utils.parsers.string.LiteralStringParser;

public class EnvelopeParseTest {


  private static final byte[] TEST_ENVELOPE = "(\"Tue, 28 Jul 2015 14:03:15 +0000\" \"Declined: weekly tennis @ Tue Jul 28, 2015 7pm - 9pm (jhuang@hubspot.com)\" ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((\"Calendar <calendar-notification@google.com>\" NIL \"Google\" NIL)) ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((NIL NIL \"jhuang\" \"hubspot.com\")) NIL NIL NIL \"<001a1135b6f27860f9051befeeb2@google.com>\"))\n".getBytes(StandardCharsets.UTF_8);
  private static final byte[] TEST_ENVELOPE_EMPTY_SUBJECT = "(\"Fri, 23 Jun 2017 17:55:45 +0200\" \"\" ((\"Test Name\" NIL \"testing0478\" \"gmail.com\")) ((\"Test Again\" NIL \"test12345\" \"gmail.com\")) ((\"Test Other\" NIL \"test456\" \"gmail.com\")) ((NIL NIL \"info\" \"test.com\")) NIL NIL NIL \"<testmessageid@mail.gmail.com>\")".getBytes(StandardCharsets.UTF_8);

  private static final NestedArrayParser.Recycler<String> ARRAY_PARSER_RECYCLER = new Recycler<>(new LiteralStringParser(new SoftReferencedAppendableCharSequence(1000), 100000));

  @Test
  public void testCanParseEnvelope() throws Exception {
    EnvelopeParser envelopeParser = new EnvelopeParser();

    NestedArrayParser<String> nestedArrayParser = ARRAY_PARSER_RECYCLER.get();
    Envelope envelope = envelopeParser.parse(nestedArrayParser.parse(wrappedBuffer(TEST_ENVELOPE)));
    nestedArrayParser.recycle();

    assertThat(envelope.getSubject()).isNotEmpty();
  }

  @Test
  public void testCanParseEnvelope2() throws Exception {
    EnvelopeParser envelopeParser = new EnvelopeParser();

    NestedArrayParser<String> nestedArrayParser = ARRAY_PARSER_RECYCLER.get();
    Envelope envelope = envelopeParser.parse(nestedArrayParser.parse(wrappedBuffer(TEST_ENVELOPE_EMPTY_SUBJECT)));
    nestedArrayParser.recycle();

    assertThat(envelope.getSubject()).isEmpty();
    assertThat(envelope.getDateString()).isNotEmpty();
  }
}
