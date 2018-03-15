package com.hubspot.imap.utils.parsers.fetch;

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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.stream.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapAddress;
import com.hubspot.imap.protocol.message.ImapAddress.Builder;
import com.hubspot.imap.utils.NilMarker;
import com.hubspot.imap.utils.enums.EnvelopeField;

public class EnvelopeParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnvelopeParser.class);
  private static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
  private static final Splitter PARAMETER_COMMA_SPLITTER = Splitter.onPattern("(\\>,)").omitEmptyStrings().trimResults();
  private static final Splitter ADDRESS_SPLITTER = Splitter.onPattern("[\\<\\>]").omitEmptyStrings().trimResults();

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
  public static Envelope parse(List<Object> in) {
    LOGGER.debug("Parsing envelope response: {}", in);

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

  public static Envelope parseHeader(Header header) {
    Map<String, String> envelopeFields = header.getFields().stream()
        .filter(f -> EnvelopeField.NAME_INDEX.containsKey(f.getName().toLowerCase()))
        .collect(Collectors.groupingBy(field -> field.getName().toLowerCase(),
            Collectors.mapping(Field::getBody, Collectors.joining(","))));

    Envelope.Builder envelope = new Envelope.Builder();

    String dateString = envelopeFields.get(EnvelopeField.DATE.getFieldName());
    List<ImapAddress> fromAddress = emailAddressesFromStringList(envelopeFields.get(EnvelopeField.FROM.getFieldName()));
    envelope.setDateString(dateString)
        .setSubject(envelopeFields.get(EnvelopeField.SUBJECT.getFieldName()))
        .setFrom(fromAddress)
        .setSender(emailAddressesFromStringList(envelopeFields.get(EnvelopeField.SENDER.getFieldName()), fromAddress))
        .setReplyTo(emailAddressesFromStringList(envelopeFields.get(EnvelopeField.REPLY_TO.getFieldName()), fromAddress))
        .setTo(emailAddressesFromStringList(envelopeFields.get(EnvelopeField.TO.getFieldName())))
        .setCc(emailAddressesFromStringList(envelopeFields.get(EnvelopeField.CC.getFieldName())))
        .setBcc(emailAddressesFromStringList(envelopeFields.get(EnvelopeField.BCC.getFieldName())))
        .setInReplyTo(envelopeFields.get(EnvelopeField.IN_REPLY_TO.getFieldName()))
        .setMessageId(envelopeFields.get(EnvelopeField.MESSAGE_ID.getFieldName()));

    try {
      if (!Strings.isNullOrEmpty(dateString)) {
        envelope.setDate(parseDate(dateString));
      }
    } catch (DateTimeParseException e) {
      LOGGER.debug("Failed to parse date {}", header.getField("date").getBody(), e);
    }
    return envelope.build();
  }

  private static List<ImapAddress> emailAddressesFromStringList(String addresses) {
    return emailAddressesFromStringList(addresses, Collections.emptyList());
  }

  @VisibleForTesting
  public static List<ImapAddress> emailAddressesFromStringList(String addresses, List<ImapAddress> defaults) {
    return Strings.isNullOrEmpty(addresses)
        ? defaults
        : getSplitter(addresses)
        .splitToList(addresses).stream()
          .map(ADDRESS_SPLITTER::splitToList)
          .map(EnvelopeParser::imapAddressFromParts)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toList());
  }

  private static Splitter getSplitter(String addresses) {
    if (addresses.contains(">")) {
      return PARAMETER_COMMA_SPLITTER;
    } else {
      return COMMA_SPLITTER;
    }
  }

  private static Optional<ImapAddress> imapAddressFromParts(List<String> addressParts) {
    if (addressParts.isEmpty()) {
      return Optional.empty();
    }


    Optional<String> emailAddressMaybe = addressParts.stream().filter(part -> part.contains("@")).findFirst();

    if (!emailAddressMaybe.isPresent()) {
      return Optional.empty();
    }

    String emailAddress = emailAddressMaybe.get();

    ImapAddress.Builder addressBuilder = new ImapAddress.Builder();
    addressBuilder.setAddress(emailAddress);
    int emailIndex = addressParts.indexOf(emailAddress);

    if (addressParts.size() > 2) {
      LOGGER.warn("Expected two address parts but found {} - {}", addressParts.size(), addressParts);
    }

    if (emailIndex > 0) {
      addressBuilder.setPersonal(addressParts.get(emailIndex - 1));
    }

    return Optional.of(addressBuilder.build());
  }

  @SuppressWarnings("unchecked")
  private static List<Object> castToList(Object object) {
    if (object instanceof String) {
      String string = ((String) object);
      if (string.startsWith("NIL")) {
        return new ArrayList<>();
      } else {
        LOGGER.debug("Failed to cast object to list: {} ", string);
        throw new IllegalStateException("A list cannot have string value other than \"NIL\"");
      }
    } else if (object instanceof NilMarker) {
      return Collections.emptyList();
    } else {
      return ((List<Object>) object);
    }
  }

  @SuppressWarnings("unchecked")
  private static String castToString(Object object) {
    if (object instanceof String) {
      return ((String) object);
    } else if (object instanceof NilMarker) {
      return null;
    } else {
      throw new IllegalStateException(String.format("Cannot use instance of type %s as string", object.getClass().getName()));
    }
  }

  @SuppressWarnings("unchecked")
  private static List<ImapAddress> emailAddressesFromNestedList(List<Object> in) {
    if (in.size() == 0) {
      return new ArrayList<>();
    }

    return in.stream()
        .filter(o -> !(o instanceof NilMarker))
        .map(o -> ((List<Object>) o).stream()
            .map(e -> {
              if (e instanceof NilMarker) {
                return null;
              } else {
                return ((String) e);
              }
            })
            .collect(Collectors.toList()))
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
