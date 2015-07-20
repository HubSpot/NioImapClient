package com.hubspot.imap.client.listener;

import com.hubspot.imap.imap.response.events.FetchEvent;

public interface FetchEventListener {
  void handle(FetchEvent event);
}
