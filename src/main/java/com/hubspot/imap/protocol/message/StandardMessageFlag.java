package com.hubspot.imap.protocol.message;

import java.util.Optional;

public enum StandardMessageFlag implements MessageFlag {
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
      return Optional.of(StandardMessageFlag.valueOf(name.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.of(new StringMessageFlag(name));
    }
  }

  public String getString() {
    return "\\" + this.name();
  }
}
