package com.hubspot.imap.protocol.response.events;

import com.hubspot.imap.protocol.response.tagged.OpenResponse;

public class OpenEvent {

  private final OpenResponse openResponse;

  public OpenEvent(OpenResponse openResponse) {
    this.openResponse = openResponse;
  }

  public OpenResponse getOpenResponse() {
    return openResponse;
  }
}
