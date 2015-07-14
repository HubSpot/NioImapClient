package com.hubspot.imap.client.listener;

public interface MessageAddListener {
  void messagesAdded(long lastMessageCount, long currentMessageCount);
}
