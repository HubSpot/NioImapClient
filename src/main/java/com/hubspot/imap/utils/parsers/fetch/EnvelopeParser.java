package com.hubspot.imap.utils.parsers.fetch;

import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnvelopeParser {
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
  public Envelope parse(List<Object> in) {
    String dateString = ((String) in.get(0));
    String subject = ((String) in.get(1));

    List<ImapAddress> from = emailAddressesFromNestedList(castToList(in.get(2)));
    List<ImapAddress> sender = emailAddressesFromNestedList(castToList(in.get(3)));
    List<ImapAddress> replyTo = emailAddressesFromNestedList(castToList(in.get(4)));
    List<ImapAddress> to = emailAddressesFromNestedList(castToList(in.get(5)));
    List<ImapAddress> cc = emailAddressesFromNestedList(castToList(in.get(6)));
    List<ImapAddress> bcc = emailAddressesFromNestedList(castToList(in.get(7)));

    String inReplyTo = ((String) in.get(8));
    String messageId = ((String) in.get(9));

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
  private List<Object> castToList(Object object) {
    if (object instanceof String) {
      String string = ((String) object);
      if (string.startsWith("NIL")) {
        return new ArrayList<>();
      } else {
        throw new IllegalStateException("A list cannot have string value other than \"NIL\"");
      }
    } else {
      return ((List<Object>) object);
    }
  }

  @SuppressWarnings("unchecked")
  private List<ImapAddress> emailAddressesFromNestedList(List<Object> in) {
    if (in.size() == 0) {
      return new ArrayList<>();
    }

    return in.stream()
        .map(o -> ((List<String>) o))
        .map(o -> new ImapAddress.Builder().parseFrom(o).build())
        .collect(Collectors.toList());
  }
}
