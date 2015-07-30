package com.hubspot.imap.utils.parsers;

import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import io.netty.util.internal.AppendableCharSequence;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.assertj.core.api.Assertions.assertThat;

public class EnvelopeParseTest {


  private static final byte[] TEST_ENVELOPE = "(\"Tue, 28 Jul 2015 14:03:15 +0000\" \"Declined: weekly tennis @ Tue Jul 28, 2015 7pm - 9pm (jhuang@hubspot.com)\" ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((\"Calendar <calendar-notification@google.com>\" NIL \"Google\" NIL)) ((\"Peter Casinelli\" NIL \"pcasinelli\" \"hubspot.com\")) ((NIL NIL \"jhuang\" \"hubspot.com\")) NIL NIL NIL \"<001a1135b6f27860f9051befeeb2@google.com>\"))\n".getBytes(StandardCharsets.UTF_8);

  @Test
  public void testCanParseEnvelope() throws Exception {
    EnvelopeParser envelopeParser = new EnvelopeParser();
    NestedArrayParser<String> nestedArrayParser = new NestedArrayParser<>(new AtomOrStringParser(new AppendableCharSequence(100000), 100000));
    Envelope envelope = envelopeParser.parse(nestedArrayParser.parse(wrappedBuffer(TEST_ENVELOPE)));
    assertThat(envelope.getSubject()).isNotEmpty();
  }
}
