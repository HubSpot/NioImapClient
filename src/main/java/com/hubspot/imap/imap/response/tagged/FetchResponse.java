package com.hubspot.imap.imap.response.tagged;

import com.hubspot.imap.imap.message.ImapMessage;

import java.util.Set;

public interface FetchResponse extends TaggedResponse {
  Set<ImapMessage> getMessages();

  class Builder extends TaggedResponse.Builder implements FetchResponse {
    private Set<ImapMessage> messages;

    public FetchResponse fromResponse(TaggedResponse response, Set<ImapMessage> messages) {
      this.messages = messages;

      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());

      return this;
    }

    @Override
    public Set<ImapMessage> getMessages() {
      return messages;
    }
  }
}
