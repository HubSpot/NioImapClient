package com.hubspot.imap.client.listener;

import java.util.concurrent.ExecutorService;

public class ListenerReference<T> {
  private final T listener;
  private final ExecutorService executorService;

  public ListenerReference(T listener, ExecutorService executorService) {
    this.listener = listener;
    this.executorService = executorService;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public T getListener() {
    return listener;
  }
}
