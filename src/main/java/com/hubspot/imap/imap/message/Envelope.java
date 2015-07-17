package com.hubspot.imap.imap.message;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public interface Envelope {
  ZonedDateTime getDate();
  String getSubject();
  List<EmailAddress> getFrom();
  List<EmailAddress> getSender();
  List<EmailAddress> getReplyTo();
  List<EmailAddress> getTo();
  List<EmailAddress> getCc();
  List<EmailAddress> getBcc();

  String getInReplyTo();
  String getMessageId();

  class Builder implements Envelope {
    private static final DateTimeFormatter RFC822_FORMATTER = DateTimeFormatter.ofPattern("[EEE, ]dd MMM yyyy HH:mm:ss Z[ (zzz)]");

    private ZonedDateTime date;
    private String subject;
    private List<EmailAddress> from;
    private List<EmailAddress> sender;
    private List<EmailAddress> replyTo;
    private List<EmailAddress> to;
    private List<EmailAddress> cc;
    private List<EmailAddress> bcc;
    private String inReplyTo;
    private String messageId;

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

    public List<EmailAddress> getFrom() {
      return this.from;
    }

    public Builder setFrom(List<EmailAddress> from) {
      this.from = from;
      return this;
    }

    public List<EmailAddress> getSender() {
      return this.sender;
    }

    public Builder setSender(List<EmailAddress> sender) {
      this.sender = sender;
      return this;
    }

    public List<EmailAddress> getReplyTo() {
      return this.replyTo;
    }

    public Builder setReplyTo(List<EmailAddress> replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public List<EmailAddress> getTo() {
      return this.to;
    }

    public Builder setTo(List<EmailAddress> to) {
      this.to = to;
      return this;
    }

    public List<EmailAddress> getCc() {
      return this.cc;
    }

    public Builder setCc(List<EmailAddress> cc) {
      this.cc = cc;
      return this;
    }

    public List<EmailAddress> getBcc() {
      return this.bcc;
    }

    public Builder setBcc(List<EmailAddress> bcc) {
      this.bcc = bcc;
      return this;
    }

    public String getInReplyTo() {
      return this.inReplyTo;
    }

    public Builder setInReplyTo(String inReplyTo) {
      this.inReplyTo = inReplyTo;
      return this;
    }

    public String getMessageId() {
      return this.messageId;
    }

    public Builder setMessageId(String messageId) {
      this.messageId = messageId;
      return this;
    }
  }
}
