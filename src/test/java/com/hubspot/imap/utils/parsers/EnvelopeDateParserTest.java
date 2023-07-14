package com.hubspot.imap.utils.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class EnvelopeDateParserTest {

  @Parameters
  public static Collection<String> parameters() {
    return Lists.newArrayList(
      "Tue, 21 Jul 2015 16:02:13 EDT",
      "Tue, 21 Jul 2015 13:48:07 -0400",
      "Sat, 11 Jul 2015 22:38:40 +0000",
      "Tue, 21 Jul 2015 20:14:36 UT",
      "Sun, 5 Jul 2015 06:43:10 +0000",
      "Thu, 23 Jul 2015 7:37:44 -0700",
      "Tue,  4 Aug 2015 17:02:29 +0000 (GMT)",
      "Sat, 8 Aug 2015 14:23:59"
    );
  }

  @Parameter
  public String input;

  @Test
  public void testDoesParseWeirdFormats() throws Exception {
    assertThat(EnvelopeParser.parseDate(input)).isNotNull();
  }
}
