package com.hubspot.imap.imap.response.events;

import com.hubspot.imap.imap.message.ImapMessage;

import java.util.Set;

public class FetchEvent {
  private Set<ImapMessage> messages;

  public FetchEvent(Set<ImapMessage> messages) {
    this.messages = messages;
  }

  public Set<ImapMessage> getMessages() {
    return messages;
  }
}
