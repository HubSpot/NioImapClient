package com.hubspot.imap.client.listener;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface MessageAddConsumer extends BiConsumer<Long, Long> {
  void accept(Long previousMessageNumber, Long currentMessageNumber);
}
