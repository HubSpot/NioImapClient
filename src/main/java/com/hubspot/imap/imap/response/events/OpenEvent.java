package com.hubspot.imap.imap.response.events;

import com.hubspot.imap.imap.response.tagged.OpenResponse;

public class OpenEvent {
  private final OpenResponse openResponse;

  public OpenEvent(OpenResponse openResponse) {
    this.openResponse = openResponse;
  }

  public OpenResponse getOpenResponse() {
    return openResponse;
  }
}
