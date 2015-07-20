package com.hubspot.imap.imap.message;

import com.google.common.base.Objects;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ImapMessage {

  Set<MessageFlag> getFlags() throws UnfetchedFieldException;
  long getMessageNumber();
  long getUid() throws UnfetchedFieldException;
  ZonedDateTime getInternalDate() throws UnfetchedFieldException;
  int getSize() throws UnfetchedFieldException;
  Envelope getEnvelope() throws UnfetchedFieldException;

  class Builder implements ImapMessage {
    private static DateTimeFormatter INTERNALDATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss Z");

    private Optional<Set<MessageFlag>> flags = Optional.empty();
    private long messageNumber;
    private Optional<Long> uid = Optional.empty();
    private Optional<ZonedDateTime> internalDate = Optional.empty();
    private Optional<Integer> size = Optional.empty();
    private Optional<Envelope> envelope = Optional.empty();

    public ImapMessage build() {
      return this;
    }

    public Set<MessageFlag> getFlags() throws UnfetchedFieldException {
      return this.flags.orElseThrow(() -> new UnfetchedFieldException("flags"));
    }

    public Builder setFlagStrings(Collection<String> flags) {
      this.flags = Optional.of(flags.stream()
          .map(MessageFlag::getFlag)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toSet()));
      return this;
    }

    public Builder setFlags(Set<MessageFlag> flags) {
      this.flags = Optional.of(flags);
      return this;
    }

    public long getMessageNumber() {
      return this.messageNumber;
    }

    public Builder setMessageNumber(long messageNumber) {
      this.messageNumber = messageNumber;
      return this;
    }

    public long getUid() throws UnfetchedFieldException {
      return this.uid.orElseThrow(() -> new UnfetchedFieldException("uid"));
    }

    public Builder setUid(long uid) {
      this.uid = Optional.of(uid);
      return this;
    }

    public ZonedDateTime getInternalDate() throws UnfetchedFieldException {
      return this.internalDate.orElseThrow(() -> new UnfetchedFieldException("internaldate"));
    }

    public Builder setInternalDate(String internalDate) {
      this.internalDate = Optional.of(ZonedDateTime.parse(internalDate, INTERNALDATE_FORMATTER));
      return this;
    }

    public int getSize() throws UnfetchedFieldException {
      return size.orElseThrow(() -> new UnfetchedFieldException("size"));
    }

    public Builder setSize(int size) {
      this.size = Optional.of(size);
      return this;
    }

    public Envelope getEnvelope() throws UnfetchedFieldException {
      return envelope.orElseThrow(() -> new UnfetchedFieldException("envelope"));
    }

    public Builder setEnvelope(Envelope envelope) {
      this.envelope = Optional.of(envelope);
      return this;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("flags", flags)
          .add("messageNumber", messageNumber)
          .add("uid", uid)
          .add("internalDate", internalDate)
          .add("size", size)
          .add("envelope", envelope)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Builder builder = (Builder) o;
      return Objects.equal(flags, builder.flags) &&
          Objects.equal(messageNumber, builder.messageNumber) &&
          Objects.equal(uid, builder.uid) &&
          Objects.equal(internalDate, builder.internalDate) &&
          Objects.equal(size, builder.size) &&
          Objects.equal(envelope, builder.envelope);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(flags, messageNumber, uid, internalDate, size, envelope);
    }
  }
}
