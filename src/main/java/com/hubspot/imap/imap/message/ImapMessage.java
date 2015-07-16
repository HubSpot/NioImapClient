package com.hubspot.imap.imap.message;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ImapMessage {

  Set<MessageFlag> getFlags() throws UnfetchedFieldException;
  long getMessageNumber() throws UnfetchedFieldException;
  long getUid() throws UnfetchedFieldException;
  ZonedDateTime getInternalDate() throws UnfetchedFieldException;
  int getSize() throws UnfetchedFieldException;

  class Builder implements ImapMessage {
    private static DateTimeFormatter INTERNALDATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss Z");

    private Optional<Set<MessageFlag>> flags = Optional.empty();
    private Optional<Long> messageNumber = Optional.empty();
    private Optional<Long> uid = Optional.empty();
    private Optional<ZonedDateTime> internalDate = Optional.empty();
    private Optional<Integer> size = Optional.empty();

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

    public long getMessageNumber() throws UnfetchedFieldException {
      return this.messageNumber.orElseThrow(() -> new UnfetchedFieldException("message number"));
    }

    public Builder setMessageNumber(long messageNumber) {
      this.messageNumber = Optional.of(messageNumber);
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
  }
}
