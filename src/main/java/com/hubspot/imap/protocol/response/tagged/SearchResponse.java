package com.hubspot.imap.protocol.response.tagged;

import com.hubspot.imap.protocol.exceptions.ResponseParseException;
import com.hubspot.imap.protocol.response.untagged.UntaggedIntResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedResponseType;

import java.util.List;
import java.util.stream.Collectors;

public interface SearchResponse extends TaggedResponse {

  List<Long> getMessageIds();

  class Builder extends TaggedResponse.Builder implements SearchResponse {
    private List<Long> messageIds;

    public SearchResponse fromResponse(TaggedResponse input) throws ResponseParseException {
      messageIds = input.getUntagged().stream()
          .filter(o -> o instanceof UntaggedIntResponse)
          .map(o -> ((UntaggedIntResponse) o))
          .filter(o -> o.getType() == UntaggedResponseType.SEARCH)
          .map(UntaggedIntResponse::getValue)
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
