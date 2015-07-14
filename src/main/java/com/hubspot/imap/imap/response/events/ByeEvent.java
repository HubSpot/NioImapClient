package com.hubspot.imap.imap.response.events;

import com.google.common.base.Objects;
import com.hubspot.imap.imap.response.untagged.UntaggedResponse;

public class ByeEvent {
  private final UntaggedResponse response;

  public ByeEvent(UntaggedResponse response) {
    this.response = response;
  }

  public UntaggedResponse getResponse() {
    return response;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("response", response)
        .toString();
  }
}
