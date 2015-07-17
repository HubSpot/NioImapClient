package com.hubspot.imap.imap.message;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public interface Envelope {
  ZonedDateTime getDate();
  String getSubject();


  class Builder implements Envelope {
    private static final DateTimeFormatter RFC822_FORMATTER = DateTimeFormatter.ofPattern("[EEE, ]dd MMM yyyy HH:mm:ss Z[ (zzz)]");

    private ZonedDateTime date;
    private String subject;

    public Envelope build() {
      return this;
    }

    public ZonedDateTime getDate() {
      return this.date;
    }

    public Builder setDate(ZonedDateTime date) {
      this.date = date;
      return this;
    }

    public Builder setDateFromString(String date) {
      if (date.startsWith("N")) { // Handle NIL
        return this;
      }

      return setDate(ZonedDateTime.parse(date, RFC822_FORMATTER));
    }

    public String getSubject() {
      return this.subject;
    }

    public Builder setSubject(String subject) {
      this.subject = subject;
      return this;
    }
  }
}
