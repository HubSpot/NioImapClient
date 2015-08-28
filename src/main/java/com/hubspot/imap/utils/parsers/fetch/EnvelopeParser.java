package com.hubspot.imap.utils.parsers.fetch;

import com.google.seventeen.common.annotations.VisibleForTesting;
import com.google.seventeen.common.base.Strings;
import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapAddress;
import com.hubspot.imap.protocol.message.ImapAddress.Builder;
import com.hubspot.imap.utils.NilMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EnvelopeParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnvelopeParser.class);

  static final DateTimeFormatter RFC2822_FORMATTER = DateTimeFormatter.ofPattern("[EEE, ]d MMM yyyy H:m:s[ zzz][ Z][ (z)]").withLocale(Locale.US);

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
    String dateString = castToString(in.get(0));
    String subject = castToString(in.get(1));

    List<ImapAddress> from = emailAddressesFromNestedList(castToList(in.get(2)));
    List<ImapAddress> sender = emailAddressesFromNestedList(castToList(in.get(3)));
    List<ImapAddress> replyTo = emailAddressesFromNestedList(castToList(in.get(4)));
    List<ImapAddress> to = emailAddressesFromNestedList(castToList(in.get(5)));
    List<ImapAddress> cc = emailAddressesFromNestedList(castToList(in.get(6)));
    List<ImapAddress> bcc = emailAddressesFromNestedList(castToList(in.get(7)));

    String inReplyTo = castToString(in.get(8));
    String messageId = castToString(in.get(9));

    Envelope.Builder envelope = new Envelope.Builder()
        .setDateString(dateString)
        .setSubject(subject)
        .setFrom(from)
        .setSender(sender)
        .setReplyTo(replyTo)
        .setTo(to)
        .setCc(cc)
        .setBcc(bcc)
        .setInReplyTo(inReplyTo)
        .setMessageId(messageId);

    try {
      if (!Strings.isNullOrEmpty(dateString) && !dateString.equalsIgnoreCase("nil")) {
        envelope.setDate(parseDate(dateString));
      }
    } catch (DateTimeParseException e) {
      LOGGER.debug("Failed to parse date {}", dateString, e);
    }

    return envelope.build();
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
    } else if (object instanceof NilMarker) {
      return Collections.emptyList();
    } else {
      return ((List<Object>) object);
    }
  }

  @SuppressWarnings("unchecked")
  private String castToString(Object object) {
    if (object instanceof String) {
      return ((String) object);
    } else if (object instanceof NilMarker) {
      return null;
    } else {
      throw new IllegalStateException(String.format("Cannot use instance of type %s as string", object.getClass().getName()));
    }
  }

  @SuppressWarnings("unchecked")
  private List<ImapAddress> emailAddressesFromNestedList(List<Object> in) {
    if (in.size() == 0) {
      return new ArrayList<>();
    }

    return in.stream()
        .map(o -> {
          if (o instanceof NilMarker) {
            return Collections.<String>emptyList();
          } else {
            return ((List<Object>) o).stream().map(e -> {
              if (e instanceof NilMarker) {
                return null;
              } else {
                return ((String) e);
              }
            }).collect(Collectors.toList());
          }
        })
        .map(o -> new Builder().parseFrom(o).build())
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  public static ZonedDateTime parseDate(String in) {
    in = in.replaceAll("\\s+", " ");

    TemporalAccessor temporalAccessor = RFC2822_FORMATTER.parseBest(in, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
    if (temporalAccessor instanceof LocalDateTime) {
      return ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
    } else if (temporalAccessor instanceof LocalDate) {
      return ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
    }
    return ((ZonedDateTime) temporalAccessor);
  }
}
