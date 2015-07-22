package com.hubspot.imap.client.listener;

import com.hubspot.imap.protocol.response.events.FetchEvent;

public interface FetchEventListener {
  void handle(FetchEvent event);
}
