package com.hubspot.imap.protocol.response.events;

import com.google.common.base.MoreObjects;
import com.hubspot.imap.protocol.response.untagged.UntaggedResponse;

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
    return MoreObjects.toStringHelper(this).add("response", response).toString();
  }
}
