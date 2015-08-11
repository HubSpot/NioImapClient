package com.hubspot.imap.protocol.message;


import java.time.ZonedDateTime;
import java.util.List;

public interface Envelope {
  ZonedDateTime getDate();
  String getSubject();
  List<ImapAddress> getFrom();
  List<ImapAddress> getSender();
  List<ImapAddress> getReplyTo();
  List<ImapAddress> getTo();
  List<ImapAddress> getCc();
  List<ImapAddress> getBcc();

  String getInReplyTo();
  String getMessageId();

  class Builder implements Envelope {

    private ZonedDateTime date;
    private String subject;
    private List<ImapAddress> from;
    private List<ImapAddress> sender;
    private List<ImapAddress> replyTo;
    private List<ImapAddress> to;
    private List<ImapAddress> cc;
    private List<ImapAddress> bcc;
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

    public String getSubject() {
      return this.subject;
    }

    public Builder setSubject(String subject) {
      this.subject = subject;
      return this;
    }

    public List<ImapAddress> getFrom() {
      return this.from;
    }

    public Builder setFrom(List<ImapAddress> from) {
      this.from = from;
      return this;
    }

    public List<ImapAddress> getSender() {
      return this.sender;
    }

    public Builder setSender(List<ImapAddress> sender) {
      this.sender = sender;
      return this;
    }

    public List<ImapAddress> getReplyTo() {
      return this.replyTo;
    }

    public Builder setReplyTo(List<ImapAddress> replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public List<ImapAddress> getTo() {
      return this.to;
    }

    public Builder setTo(List<ImapAddress> to) {
      this.to = to;
      return this;
    }

    public List<ImapAddress> getCc() {
      return this.cc;
    }

    public Builder setCc(List<ImapAddress> cc) {
      this.cc = cc;
      return this;
    }

    public List<ImapAddress> getBcc() {
      return this.bcc;
    }

    public Builder setBcc(List<ImapAddress> bcc) {
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
