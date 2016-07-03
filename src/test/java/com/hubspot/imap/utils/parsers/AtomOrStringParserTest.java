package com.hubspot.imap.utils.parsers;


import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;

public class AtomOrStringParserTest {
  private static final SoftReferencedAppendableCharSequence SEQUENCE_REF = new SoftReferencedAppendableCharSequence(1000);
  private static final AtomOrStringParser PARSER = new AtomOrStringParser(SEQUENCE_REF, 1000);

  private static final byte[] UNQUOTED = "Unquoted".getBytes(StandardCharsets.UTF_8);
  private static final byte[] QUOTED = "\"This is quoted\"".getBytes(StandardCharsets.UTF_8);
  private static final byte[] QUOTED_RESULT = Arrays.copyOfRange(QUOTED, 1, QUOTED.length-1);
  private static final byte[] ESCAPED_QUOTES = "\"This contains \\\"quotes\\\"\"".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ESCAPED_QUOTES_RESULT = Arrays.copyOfRange(ESCAPED_QUOTES, 1, ESCAPED_QUOTES.length - 1);

  @Test
  public void testGivenUnquotedString_doesReturnWholeString() throws Exception {
    String result = PARSER.parse(wrappedBuffer(UNQUOTED));
    assertThat(result.getBytes(StandardCharsets.UTF_8)).isEqualTo(UNQUOTED);
  }

  @Test
  public void testGivenQuotedString_doesReturnStringWithoutQuotes() throws Exception {
    String result = PARSER.parse(wrappedBuffer(QUOTED));
    assertThat(result.getBytes(StandardCharsets.UTF_8)).isEqualTo(QUOTED_RESULT);
  }

  @Test
  public void testGivenStringWithEscapedQuotesQuotedString_doesReturnStringWithoutSurroundingQuotes() throws Exception {
    String result = PARSER.parse(wrappedBuffer(ESCAPED_QUOTES));
    assertThat(result.getBytes(StandardCharsets.UTF_8)).isEqualTo(ESCAPED_QUOTES_RESULT);
  }
}
