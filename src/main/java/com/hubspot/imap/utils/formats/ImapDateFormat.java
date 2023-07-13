package com.hubspot.imap.utils.formats;

import com.google.common.collect.ImmutableSet;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Set;

public class ImapDateFormat {

  public static final DateTimeFormatter IMAP_FULL_DATE_FORMAT = new DateTimeFormatterBuilder()
    .appendPattern("dd-MMM-yyyy ")
    .appendPattern("HH:mm:ss ")
    .appendOffset("+HHMM", "+0000")
    .toFormatter(Locale.US);

  /** Format date without time or timezone*/
  private static final DateTimeFormatter IMAP_SHORT_DATE_FORMAT = new DateTimeFormatterBuilder()
    .appendPattern("dd-MMM-yyyy ")
    .toFormatter(Locale.US);

  public static final DateTimeFormatter INTERNALDATE_FORMATTER = DateTimeFormatter.ofPattern(
    "d-MMM-yyyy HH:mm:ss Z"
  );
  private static final DateTimeFormatter INTERNALDATE_FORMATTER_WITH_ZONE = DateTimeFormatter.ofPattern(
    "d-MMM-yyyy HH:mm:ss Z z"
  );
  private static final Set<DateTimeFormatter> INTERNALDATE_FORMATTERS = ImmutableSet.of(
    INTERNALDATE_FORMATTER,
    INTERNALDATE_FORMATTER_WITH_ZONE
  );

  public static String toImapDateWithTimeString(ZonedDateTime d) {
    return IMAP_FULL_DATE_FORMAT.format(d);
  }

  public static String toImapDateOnlyString(ZonedDateTime d) {
    return IMAP_SHORT_DATE_FORMAT.format(d);
  }

  public static ZonedDateTime fromStringToZonedDateTime(String dateString) {
    for (DateTimeFormatter formatter : INTERNALDATE_FORMATTERS) {
      try {
        return ZonedDateTime.parse(dateString.trim(), formatter);
      } catch (Exception e) {
        // swallow
      }
    }

    throw new DateTimeException(
      String.format("Failed to parse date string: '%s'", dateString)
    );
  }
}
