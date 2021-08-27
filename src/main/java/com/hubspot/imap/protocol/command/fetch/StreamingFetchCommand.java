package com.hubspot.imap.protocol.command.fetch;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.hubspot.imap.client.listener.ListenerReference;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;
import com.hubspot.imap.protocol.message.ImapMessage;

public class StreamingFetchCommand<R> extends FetchCommand {

  private final ListenerReference<Function<ImapMessage, R>> messageConsumer;

  public StreamingFetchCommand(long startId, Optional<Long> stopId, ListenerReference<Function<ImapMessage, R>> messageConsumer, List<FetchDataItem> fetchDataItems) {
    super(startId, stopId, fetchDataItems);

    this.messageConsumer = messageConsumer;
  }

  public StreamingFetchCommand(long startId, Optional<Long> stopId, ListenerReference<Function<ImapMessage, R>> messageConsumer, FetchDataItem item, FetchDataItem... otherItems) {
    this(startId, stopId, messageConsumer, Lists.asList(item, otherItems));
  }

  public CompletableFuture<?> invokeListener(ImapMessage message) {
    return CompletableFuture.supplyAsync(() ->
        messageConsumer.getListener().apply(message), messageConsumer.getExecutorService());
  }
}
