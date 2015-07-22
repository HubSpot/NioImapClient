package com.hubspot.imap.client;

import com.hubspot.imap.client.listener.FetchEventListener;
import com.hubspot.imap.client.listener.MessageAddListener;
import com.hubspot.imap.client.listener.OpenEventListener;
import com.hubspot.imap.protocol.command.Command;
import com.hubspot.imap.protocol.response.events.ExistsEvent;
import com.hubspot.imap.protocol.response.events.ExpungeEvent;
import com.hubspot.imap.protocol.response.events.FetchEvent;
import com.hubspot.imap.protocol.response.events.OpenEvent;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ImapClientState extends ChannelInboundHandlerAdapter {
  private final EventExecutorGroup executorGroup;

  private final AtomicReference<Command> currentCommand;
  private final AtomicLong commandCount;
  private final AtomicLong messageNumber;

  private final List<MessageAddListener> messageAddListeners;
  private final List<FetchEventListener> fetchEventListeners;
  private final List<OpenEventListener> openEventListeners;

  public ImapClientState(EventExecutorGroup executorGroup) {
    this.executorGroup = executorGroup;

    this.currentCommand = new AtomicReference<>();
    this.commandCount = new AtomicLong(0);
    this.messageNumber = new AtomicLong(0);

    this.messageAddListeners = new ArrayList<>();
    this.fetchEventListeners = new ArrayList<>();
    this.openEventListeners = new ArrayList<>();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof ExpungeEvent) {
      messageNumber.decrementAndGet();
    } else if (evt instanceof ExistsEvent) {
      ExistsEvent exists = ((ExistsEvent) evt);
      long lastMessageCount = messageNumber.getAndSet(exists.getValue());
      long currentCount = messageNumber.get();
      if (currentCount > lastMessageCount) {
        for (MessageAddListener listener: messageAddListeners) {
          executorGroup.submit(() -> listener.messagesAdded(lastMessageCount, currentCount));
        }
      }
    } else if (evt instanceof FetchEvent) {
      FetchEvent fetchEvent = ((FetchEvent) evt);
      for (FetchEventListener listener: fetchEventListeners) {
        executorGroup.submit(() -> listener.handle(fetchEvent));
      }
    } else if (evt instanceof OpenEvent) {
      OpenEvent event = ((OpenEvent) evt);
      OpenResponse response = event.getOpenResponse();
      messageNumber.set(response.getExists());

      for (OpenEventListener listener: openEventListeners) {
        executorGroup.submit(() -> listener.handle(event));
      }
    }

    super.userEventTriggered(ctx, evt);
  }

  public void onMessageAdd(MessageAddListener listener) {
    this.messageAddListeners.add(listener);
  }

  public void addFetchEventListener(FetchEventListener listener) {
    this.fetchEventListeners.add(listener);
  }

  public void addOpenEventListener(OpenEventListener listener) {
    this.openEventListeners.add(listener);
  }

  public long getMessageNumber() {
    return messageNumber.get();
  }

  public String getNextTag() {
    return String.format("A%03d ", commandCount.getAndIncrement());
  }

  public Command getCurrentCommand() {
    return currentCommand.get();
  }

  public void setCurrentCommand(Command command) {
    currentCommand.set(command);
  }
}
