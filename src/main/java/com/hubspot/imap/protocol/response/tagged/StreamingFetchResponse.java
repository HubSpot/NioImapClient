package com.hubspot.imap.protocol.response.tagged;

import java.util.List;
import java.util.stream.Collectors;

import io.netty.util.concurrent.Future;

public interface StreamingFetchResponse<T> extends TaggedResponse {

  List<Future<T>> getMessageConsumerFutures();

  class Builder<T> extends TaggedResponse.Builder implements StreamingFetchResponse {
    private List<Future<T>> messageConsumerFutures;

    public StreamingFetchResponse fromResponse(TaggedResponse response) {
      this.messageConsumerFutures = filterFutures(response);

      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());

      return this;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<Future<T>> filterFutures(TaggedResponse response) {
      return response.getUntagged().stream()
          .filter(m -> m instanceof Future)
          .map(m -> ((Future<T>) m))
          .collect(Collectors.toList());
    }

    @Override
    public List<Future<T>> getMessageConsumerFutures() {
      return messageConsumerFutures;
    }
  }

}
