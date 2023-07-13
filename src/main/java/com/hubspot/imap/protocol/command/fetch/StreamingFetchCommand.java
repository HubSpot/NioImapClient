package com.hubspot.imap.protocol.command.fetch;

import com.google.common.collect.Lists;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;
import com.hubspot.imap.protocol.message.ImapMessage;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class StreamingFetchCommand<R> extends FetchCommand {

  private final Function<ImapMessage, R> messageConsumer;

  public StreamingFetchCommand(
    long startId,
    Optional<Long> stopId,
    Function<ImapMessage, R> messageConsumer,
    List<FetchDataItem> fetchDataItems
  ) {
    super(startId, stopId, fetchDataItems);
    this.messageConsumer = messageConsumer;
  }

  public StreamingFetchCommand(
    long startId,
    Optional<Long> stopId,
    Function<ImapMessage, R> messageConsumer,
    FetchDataItem item,
    FetchDataItem... otherItems
  ) {
    this(startId, stopId, messageConsumer, Lists.asList(item, otherItems));
  }

  public R handle(ImapMessage message) {
    return messageConsumer.apply(message);
  }
}
