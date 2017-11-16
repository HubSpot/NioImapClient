package com.hubspot.imap.utils.formats;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class ImapDateFormat {
  private static final DateTimeFormatter IMAP_FULL_DATE_FORMAT =
    new DateTimeFormatterBuilder()
      .appendPattern("dd-MMM-yyyy ")
      .appendPattern("HH:mm:ss ")
      .appendOffset("+HHMM", "+0000")
      .toFormatter(Locale.US);

  /** Format date without time or timezone*/
  private static final DateTimeFormatter IMAP_SHORT_DATE_FORMAT =
    new DateTimeFormatterBuilder()
      .appendPattern("dd-MMM-yyyy ")
      .toFormatter(Locale.US);

  public static final DateTimeFormatter INTERNALDATE_FORMATTER = DateTimeFormatter.ofPattern("d-MMM-yyyy HH:mm:ss Z");

  public static String toImapDateWithTimeString(ZonedDateTime d) {
    return IMAP_FULL_DATE_FORMAT.format(d);
  }

  public static String toImapDateOnlyString(ZonedDateTime d) {
    return IMAP_SHORT_DATE_FORMAT.format(d);
  }
}
