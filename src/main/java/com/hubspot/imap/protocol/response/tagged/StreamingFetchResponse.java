package com.hubspot.imap.protocol.response.tagged;

import java.util.List;
import java.util.stream.Collectors;

import io.netty.util.concurrent.Future;

public interface StreamingFetchResponse extends TaggedResponse {

  List<Future<?>> getMessageConsumerFutures();

  class Builder extends TaggedResponse.Builder implements StreamingFetchResponse {
    private List<Future<?>> messageConsumerFutures;

    public StreamingFetchResponse fromResponse(TaggedResponse response) {
      this.messageConsumerFutures = filterFutures(response);

      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());

      return this;
    }

    @SuppressWarnings("unchecked")
    private static List<Future<?>> filterFutures(TaggedResponse response) {
      return response.getUntagged().stream()
          .filter(m -> m instanceof Future)
          .map(m -> ((Future<?>) m))
          .collect(Collectors.toList());
    }

    @Override
    public List<Future<?>> getMessageConsumerFutures() {
      return messageConsumerFutures;
    }
  }

}
