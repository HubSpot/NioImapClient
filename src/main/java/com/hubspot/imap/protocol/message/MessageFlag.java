package com.hubspot.imap.protocol.message;

import java.util.Optional;

public enum  MessageFlag {
  SEEN,
  ANSWERED,
  FLAGGED,
  DELETED,
  DRAFT,
  RECENT,
  INVALID;

  public static Optional<MessageFlag> getFlag(String name) {
    if (name.startsWith("\\") || name.startsWith("$")) {
      name = name.substring(1);
    }

    try {
      return Optional.of(MessageFlag.valueOf(name.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
