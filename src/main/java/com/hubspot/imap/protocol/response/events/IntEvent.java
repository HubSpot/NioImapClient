package com.hubspot.imap.protocol.response.events;

import com.google.common.base.MoreObjects;

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
    return MoreObjects.toStringHelper(this)
        .add("value", value)
        .toString();
  }
}
