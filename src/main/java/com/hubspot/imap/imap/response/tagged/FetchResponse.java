package com.hubspot.imap.imap.response.tagged;

import com.hubspot.imap.imap.message.ImapMessage;

import java.util.Set;
import java.util.stream.Collectors;

public interface FetchResponse extends TaggedResponse {
  Set<ImapMessage> getMessages();

  class Builder extends TaggedResponse.Builder implements FetchResponse {
    private Set<ImapMessage> messages;

    public FetchResponse fromResponse(TaggedResponse response) {
      this.messages = response.getUntagged().stream()
          .filter(u -> u instanceof ImapMessage).map(u -> ((ImapMessage) u))
          .collect(Collectors.toSet());

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
