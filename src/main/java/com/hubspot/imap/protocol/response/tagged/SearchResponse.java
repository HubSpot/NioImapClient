package com.hubspot.imap.protocol.response.tagged;

import com.hubspot.imap.protocol.exceptions.ResponseParseException;
import com.hubspot.imap.protocol.response.untagged.UntaggedSearchResponse;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface SearchResponse extends TaggedResponse {

  List<Long> getMessageIds();

  class Builder extends TaggedResponse.Builder implements SearchResponse {
    private List<Long> messageIds;

    public SearchResponse fromResponse(TaggedResponse input) throws ResponseParseException {
      messageIds = input.getUntagged().stream()
          .filter(o -> o instanceof UntaggedSearchResponse)
          .map(o -> ((UntaggedSearchResponse) o))
          .map(UntaggedSearchResponse::getIds)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());

      setCode(input.getCode());
      setMessage(input.getMessage());
      setTag(input.getTag());

      return this;
    }

    public List<Long> getMessageIds() {
      return messageIds;
    }
  }
}
