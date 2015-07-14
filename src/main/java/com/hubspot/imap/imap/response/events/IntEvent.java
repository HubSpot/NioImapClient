package com.hubspot.imap.imap.response.events;

import com.google.common.base.Objects;

public abstract class IntEvent {
  private long value;

  public IntEvent(long value) {
    this.value = value;
  }

  public long getValue() {
    return value;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("value", value)
        .toString();
  }
}
