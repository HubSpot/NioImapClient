package com.hubspot.imap.protocol.message;

import com.google.seventeen.common.collect.Lists;
import com.hubspot.imap.protocol.message.Envelope.Builder;
import org.junit.Test;

import java.util.List;

public class EnvelopeTest {

  private static final List<String> INPUT = Lists.newArrayList("Tue, 21 Jul 2015 16:02:13 EDT", "Tue, 21 Jul 2015 13:48:07 -0400", "Sat, 11 Jul 2015 22:38:40 +0000", "Tue, 21 Jul 2015 20:14:36 UT", "Sun, 5 Jul 2015 06:43:10 +0000");

  @Test
  public void testDoesParseWeirdFormats() throws Exception {
    INPUT.forEach(Builder.RFC2822_FORMATTER::parse);
  }
}
