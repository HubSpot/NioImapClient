package com.hubspot.imap.protocol.response.tagged;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface StreamingFetchResponse<T> extends TaggedResponse {

  List<CompletableFuture<T>> getMessageConsumerFutures();

  class Builder<T> extends TaggedResponse.Builder implements StreamingFetchResponse {
    private List<CompletableFuture<T>> messageConsumerFutures;

    public StreamingFetchResponse fromResponse(TaggedResponse response) {
      this.messageConsumerFutures = filterFutures(response);

      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());

      return this;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<CompletableFuture<T>> filterFutures(TaggedResponse response) {
      return response.getUntagged().stream()
          .filter(m -> m instanceof CompletableFuture)
          .map(m -> ((CompletableFuture<T>) m))
          .collect(Collectors.toList());
    }

    @Override
    public List<CompletableFuture<T>> getMessageConsumerFutures() {
      return messageConsumerFutures;
    }
  }

}
