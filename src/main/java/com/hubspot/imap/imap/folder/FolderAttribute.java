package com.hubspot.imap.imap.folder;

import java.util.Optional;

public enum FolderAttribute {
  ALL,
  NOINFERIORS,
  NOSELECT,
  MARKED,
  UNMARKED,
  HASNOCHILDREN,
  DRAFTS,
  IMPORTANT,
  SENT,
  JUNK,
  TRASH;

  public static Optional<FolderAttribute> getAttribute(String name) {
    if (name.startsWith("\\")) {
      name = name.substring(1);
    }

    try {
      return Optional.of(FolderAttribute.valueOf(name.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
