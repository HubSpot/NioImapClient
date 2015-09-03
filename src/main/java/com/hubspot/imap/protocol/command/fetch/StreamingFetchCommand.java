package com.hubspot.imap.protocol.command.fetch;

import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;
import com.hubspot.imap.protocol.message.ImapMessage;

import java.util.Optional;
import java.util.function.Consumer;

public class StreamingFetchCommand extends FetchCommand {

  private final Consumer<ImapMessage> messageConsumer;

  public StreamingFetchCommand(long startId, Optional<Long> stopId, Consumer<ImapMessage> messageConsumer, FetchDataItem... fetchDataItems) {
    super(startId, stopId, fetchDataItems);

    this.messageConsumer = messageConsumer;
  }

  public void handle(ImapMessage message) {
    messageConsumer.accept(message);
  }
}
