package com.hubspot.imap.utils.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapAddress;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class EnvelopeParserTest {

  @Test
  public void testParseWithAllElements() {
    List<Object> envelopeResponse = Arrays.asList(
      "Tue, 21 Jul 2015 16:02:13 EDT", // date
      "Test Subject", // subject
      Arrays.asList(List.of("John Doe", "smtp", "john.doe", "example.com")), // from
      Arrays.asList(List.of("Jane Doe", "smtp", "jane.doe", "example.com")), // sender
      Arrays.asList(List.of("Reply To", "smtp", "reply.to", "example.com")), // reply-to
      Arrays.asList(List.of("Recipient", "smtp", "recipient", "example.com")), // to
      Arrays.asList(List.of("CC", "smtp", "cc", "example.com")), // cc
      Arrays.asList(List.of("BCC", "smtp", "bcc", "example.com")), // bcc
      "In-Reply-To", // in-reply-to
      "Message-ID" // message-id
    );

    Envelope envelope = EnvelopeParser.parse(envelopeResponse);

    assertThat(envelope.getDateString()).isEqualTo("Tue, 21 Jul 2015 16:02:13 EDT");
    assertThat(envelope.getSubject()).isEqualTo("Test Subject");
    assertThat(envelope.getFrom())
      .extracting(ImapAddress::getAddress)
      .containsExactly("john.doe@example.com");
    assertThat(envelope.getSender())
      .extracting(ImapAddress::getAddress)
      .containsExactly("jane.doe@example.com");
    assertThat(envelope.getReplyTo())
      .extracting(ImapAddress::getAddress)
      .containsExactly("reply.to@example.com");
    assertThat(envelope.getTo())
      .extracting(ImapAddress::getAddress)
      .containsExactly("recipient@example.com");
    assertThat(envelope.getCc())
      .extracting(ImapAddress::getAddress)
      .containsExactly("cc@example.com");
    assertThat(envelope.getBcc())
      .extracting(ImapAddress::getAddress)
      .containsExactly("bcc@example.com");
    assertThat(envelope.getInReplyTo()).isEqualTo("In-Reply-To");
    assertThat(envelope.getMessageId()).isEqualTo("Message-ID");
  }

  @Test
  public void testParseWithOnly9Elements() {
    List<Object> envelopeResponse = Arrays.asList(
      "Tue, 21 Jul 2015 16:02:13 EDT", // date
      "Test Subject", // subject
      Arrays.asList(List.of("John Doe", "smtp", "john.doe", "example.com")), // from
      Arrays.asList(List.of("Jane Doe", "smtp", "jane.doe", "example.com")), // sender
      Arrays.asList(List.of("Reply To", "smtp", "reply.to", "example.com")), // reply-to
      Arrays.asList(List.of("Recipient", "smtp", "recipient", "example.com")), // to
      Arrays.asList(List.of("CC", "smtp", "cc", "example.com")), // cc
      Arrays.asList(List.of("BCC", "smtp", "bcc", "example.com")), // bcc
      // missing in-reply-to
      "Message-ID" // message-id
    );

    assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> {
        EnvelopeParser.parse(envelopeResponse);
      }
    );
  }
}
