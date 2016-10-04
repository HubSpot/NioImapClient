package com.hubspot.imap.utils.parsers;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.NestedArrayParser.Recycler;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import com.hubspot.imap.utils.parsers.string.AtomOrStringParser;

public class EnvelopeParseTest {


  private static final byte[] TEST_ENVELOPE = "(\"Tue, 28 Jul 2015 14:03:15 +0000\" \"Declined: weekly tennis @ Tue Jul 28, 2015 7pm - 9pm (jhuang@hubspot.com)\" ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((\"Calendar <calendar-notification@google.com>\" NIL \"Google\" NIL)) ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((NIL NIL \"jhuang\" \"hubspot.com\")) NIL NIL NIL \"<001a1135b6f27860f9051befeeb2@google.com>\"))\n".getBytes(StandardCharsets.UTF_8);

  @Test
  public void testCanParseEnvelope() throws Exception {
    NestedArrayParser.Recycler<String> arrayParserRecycler = new Recycler<>(new AtomOrStringParser(new SoftReferencedAppendableCharSequence(1000), 100000));
    EnvelopeParser envelopeParser = new EnvelopeParser();

    NestedArrayParser<String> nestedArrayParser = arrayParserRecycler.get();
    Envelope envelope = envelopeParser.parse(nestedArrayParser.parse(wrappedBuffer(TEST_ENVELOPE)));
    nestedArrayParser.recycle();

    assertThat(envelope.getSubject()).isNotEmpty();
  }
}
