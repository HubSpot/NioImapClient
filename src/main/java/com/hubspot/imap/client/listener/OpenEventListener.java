package com.hubspot.imap.client.listener;

import com.hubspot.imap.imap.response.events.OpenEvent;

public interface OpenEventListener {
  void handle(OpenEvent event);
}
