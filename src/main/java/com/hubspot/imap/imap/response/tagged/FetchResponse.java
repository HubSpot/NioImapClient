package com.hubspot.imap.imap.response.tagged;

import com.hubspot.imap.imap.command.fetch.FetchCommand;
import com.hubspot.imap.imap.message.ImapMessage;

import java.util.Set;
import java.util.stream.Collectors;

public interface FetchResponse extends TaggedResponse {
  Set<ImapMessage> getMessages();

  class Builder extends TaggedResponse.Builder implements FetchResponse {
    private Set<ImapMessage> messages;

    public FetchResponse fromResponse(FetchCommand command, TaggedResponse response) {
      this.messages = filterFetchedMessages(command, response);

      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());

      return this;
    }

    private static Set<ImapMessage> filterFetchedMessages(FetchCommand fetchCommand, TaggedResponse response) {
      return response.getUntagged().stream()
          .filter(m -> m instanceof ImapMessage)
          .map(m -> ((ImapMessage) m))
          .filter(m -> {
            if (fetchCommand.getStopId().isPresent()) {
              return m.getMessageNumber() >= fetchCommand.getStartId() &&
                  m.getMessageNumber() <= fetchCommand.getStopId().get();
            } else {
              return m.getMessageNumber() >= fetchCommand.getStartId();
            }
          }).collect(Collectors.toSet());
    }

    @Override
    public Set<ImapMessage> getMessages() {
      return messages;
    }
  }
}
