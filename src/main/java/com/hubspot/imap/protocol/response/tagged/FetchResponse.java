package com.hubspot.imap.protocol.response.tagged;

import com.hubspot.imap.protocol.message.ImapMessage;
import java.util.Set;
import java.util.stream.Collectors;

public interface FetchResponse extends TaggedResponse {
  Set<ImapMessage> getMessages();

  class Builder extends TaggedResponse.Builder implements FetchResponse {

    private Set<ImapMessage> messages;

    public FetchResponse fromResponse(TaggedResponse response) {
      this.messages = filterFetchedMessages(response);

      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());

      return this;
    }

    private static Set<ImapMessage> filterFetchedMessages(TaggedResponse response) {
      return response
        .getUntagged()
        .stream()
        .filter(m -> m instanceof ImapMessage)
        .map(m -> ((ImapMessage) m))
        .collect(Collectors.toSet());
    }

    @Override
    public Set<ImapMessage> getMessages() {
      return messages;
    }
  }
}
