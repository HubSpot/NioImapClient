package com.hubspot.imap.protocol.folder;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class FolderFlags {
  public enum Flag {
    ANSWERED,
    FLAGGED,
    DRAFT,
    DELETED,
    SEEN,
    NOTPHISHING,
    STAR,
    PHISHING;
  }

  private final Set<Flag> flags;
  private final boolean permanent;

  public FolderFlags(Set<Flag> flags, boolean permanent) {
    this.flags = flags;
    this.permanent = permanent;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public boolean isPermanent() {
    return permanent;
  }

  public static Optional<Flag> getFlag(String name) {
    if (name.startsWith("\\") || name.startsWith("$")) {
      name = name.substring(1);
    }

    if (name.equals("*")) {
      return Optional.of(Flag.STAR);
    }

    try {
      return Optional.of(Flag.valueOf(name.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public static FolderFlags fromStrings(Collection<String> input, boolean permanent) {
    return new FolderFlags(
        input.stream()
            .map(FolderFlags::getFlag)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet()),
        permanent
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FolderFlags that = (FolderFlags) o;
    return Objects.equal(isPermanent(), that.isPermanent()) &&
        Objects.equal(getFlags(), that.getFlags());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getFlags(), isPermanent());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("flags", flags)
        .add("permanent", permanent)
        .toString();
  }
}
