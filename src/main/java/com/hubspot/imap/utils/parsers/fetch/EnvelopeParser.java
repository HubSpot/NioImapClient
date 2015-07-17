package com.hubspot.imap.utils.parsers.fetch;

import com.hubspot.imap.imap.message.Envelope;
import com.hubspot.imap.utils.parsers.MatchingParenthesesParser;
import com.hubspot.imap.utils.parsers.NestedArrayParser;
import com.hubspot.imap.utils.parsers.OptionallyQuotedStringParser;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.AppendableCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EnvelopeParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnvelopeParser.class);

  private final OptionallyQuotedStringParser quotedStringParser;
  private final MatchingParenthesesParser matchingParenthesesParser;
  private final NestedArrayParser<String> nestedArrayParser;

  public EnvelopeParser(AppendableCharSequence seq,
                        OptionallyQuotedStringParser quotedStringParser,
                        MatchingParenthesesParser matchingParenthesesParser) {
    this.quotedStringParser = quotedStringParser;
    this.matchingParenthesesParser = matchingParenthesesParser;
    this.nestedArrayParser = new NestedArrayParser<>(seq, quotedStringParser);
  }

  /**
   * This parses an envelope response according to RFC3501:
   *
   *     The fields of the envelope structure are in the following
   *     order: date, subject, from, sender, reply-to, to, cc, bcc,
   *     in-reply-to, and message-id.  The date, subject, in-reply-to,
   *     and message-id fields are strings.  The from, sender, reply-to,
   *     to, cc, and bcc fields are parenthesized lists of address
   *     structures.
   *
   * @param in ByteBuf containing the full envelope response.
   * @return Parsed Envelope object.
   */
  public Envelope parse(ByteBuf in) {
    Envelope.Builder builder = new Envelope.Builder();

    String dateString = quotedStringParser.parse(in);
    builder.setDateFromString(dateString);

    String subject = quotedStringParser.parse(in);
    builder.setSubject(subject);

    List<Object> from = nestedArrayParser.parse(in);

    List<Object> sender = nestedArrayParser.parse(in);

    return builder.build();
  }
}
