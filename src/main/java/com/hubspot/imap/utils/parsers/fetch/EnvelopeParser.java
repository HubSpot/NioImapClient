package com.hubspot.imap.utils.parsers.fetch;

import com.hubspot.imap.imap.message.EmailAddress;
import com.hubspot.imap.imap.message.Envelope;
import com.hubspot.imap.utils.parsers.MatchingParenthesesParser;
import com.hubspot.imap.utils.parsers.NestedArrayParser;
import com.hubspot.imap.utils.parsers.OptionallyQuotedStringParser;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.AppendableCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    String dateString = quotedStringParser.parse(in);
    String subject = quotedStringParser.parse(in);

    List<EmailAddress> from = emailAddressesFromNestedList(nestedArrayParser.parse(in));
    List<EmailAddress> sender = emailAddressesFromNestedList(nestedArrayParser.parse(in));
    List<EmailAddress> replyTo = emailAddressesFromNestedList(nestedArrayParser.parse(in));
    List<EmailAddress> to = emailAddressesFromNestedList(nestedArrayParser.parse(in));
    List<EmailAddress> cc = emailAddressesFromNestedList(nestedArrayParser.parse(in));
    List<EmailAddress> bcc = emailAddressesFromNestedList(nestedArrayParser.parse(in));

    String inReplyTo = quotedStringParser.parse(in);
    String messageId = quotedStringParser.parse(in);

    Envelope envelope = new Envelope.Builder()
        .setDateFromString(dateString)
        .setSubject(subject)
        .setFrom(from)
        .setSender(sender)
        .setReplyTo(replyTo)
        .setTo(to)
        .setCc(cc)
        .setBcc(bcc)
        .setInReplyTo(inReplyTo)
        .setMessageId(messageId)
        .build();

    return envelope;
  }

  @SuppressWarnings("unchecked")
  private List<EmailAddress> emailAddressesFromNestedList(List<Object> in) {
    if (in.size() == 0) {
      return new ArrayList<>();
    }

    return in.stream()
        .map(o -> ((List<String>) o))
        .map(o -> new EmailAddress.Builder().parseFrom(o).build())
        .collect(Collectors.toList());
  }
}
