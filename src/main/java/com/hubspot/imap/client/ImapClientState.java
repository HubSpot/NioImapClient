package com.hubspot.imap.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hubspot.imap.client.listener.ConnectionListener;
import com.hubspot.imap.client.listener.ListenerReference;
import com.hubspot.imap.client.listener.MessageAddConsumer;
import com.hubspot.imap.protocol.command.ImapCommand;
import com.hubspot.imap.protocol.response.events.ExistsEvent;
import com.hubspot.imap.protocol.response.events.ExpungeEvent;
import com.hubspot.imap.protocol.response.events.OpenEvent;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;

public class ImapClientState extends ChannelInboundHandlerAdapter {
  private final String clientName;
  private final EventExecutorGroup executorGroup;

  private final AtomicReference<ImapCommand> currentCommand;
  private final AtomicLong commandCount;
  private final AtomicLong messageNumber;

  private final List<ListenerReference<MessageAddConsumer>> messageAddListeners;
  private final List<ListenerReference<Consumer<OpenEvent>>> openEventListeners;
  private final List<ConnectionListener> connectionListeners;
  private final List<ChannelHandler> handlers;

  private Channel channel;

  public ImapClientState(String clientName, EventExecutorGroup executorGroup) {
    this.clientName = clientName;
    this.executorGroup = executorGroup;

    this.currentCommand = new AtomicReference<>();
    this.commandCount = new AtomicLong(0);
    this.messageNumber = new AtomicLong(0);

    this.messageAddListeners = new CopyOnWriteArrayList<>();
    this.openEventListeners = new CopyOnWriteArrayList<>();
    this.connectionListeners = new CopyOnWriteArrayList<>();
    this.handlers = new CopyOnWriteArrayList<>();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.channel = ctx.channel();

    handlers.forEach(h -> channel.pipeline().addLast(executorGroup, h));
    connectionListeners.forEach(w -> channel.pipeline().addLast(executorGroup, w));
    super.channelActive(ctx);
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
        for (ListenerReference<MessageAddConsumer> listenerReference: messageAddListeners) {
          MessageAddConsumer listener = listenerReference.getListener();
          listenerReference.getExecutorService().submit(() -> listener.accept(lastMessageCount, currentCount));
        }
      }
    } else if (evt instanceof OpenEvent) {
      OpenEvent event = ((OpenEvent) evt);
      OpenResponse response = event.getOpenResponse();
      messageNumber.set(response.getExists());

      for (ListenerReference<Consumer<OpenEvent>> listenerReference : openEventListeners) {
        listenerReference.getExecutorService().submit(() -> listenerReference.getListener().accept(event));
      }
    }

    super.userEventTriggered(ctx, evt);
  }

  @Deprecated
  public void onMessageAdd(MessageAddConsumer consumer) {
    this.messageAddListeners.add(new ListenerReference<>(consumer, executorGroup));
  }

  public void onMessageAdd(MessageAddConsumer consumer, ExecutorService executorService) {
    this.messageAddListeners.add(new ListenerReference<>(consumer, executorService));
  }

  @Deprecated
  public void addOpenEventListener(Consumer<OpenEvent> consumer) {
    this.openEventListeners.add(new ListenerReference<>(consumer, executorGroup));
  }

  public void addOpenEventListener(Consumer<OpenEvent> consumer, ExecutorService executorService) {
    this.openEventListeners.add(new ListenerReference<>(consumer, executorService));
  }

  public void addHandler(ChannelHandler handler) {
    if (channel != null) {
      channel.pipeline().addLast(executorGroup, handler);
    }

    handlers.add(handler);
  }

  public void addConnectionListener(ConnectionListener listener) {
    if (channel != null) {
      channel.pipeline().addLast(executorGroup, listener);
    }

    connectionListeners.add(listener);
  }

  public long getMessageNumber() {
    return messageNumber.get();
  }

  public String getNextTag() {
    return String.format("A%03d ", commandCount.getAndIncrement());
  }

  public ImapCommand getCurrentCommand() {
    return currentCommand.get();
  }

  public void setCurrentCommand(ImapCommand imapCommand) {
    currentCommand.set(imapCommand);
  }

  public String getClientName() {
    return clientName;
  }
}
