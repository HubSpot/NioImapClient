package com.hubspot.imap;

import java.util.concurrent.ThreadFactory;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

@Immutable
@Style(
    typeAbstract = {"*IF"},
    typeImmutable = "*"
)
@JsonDeserialize(as = ImapClientFactoryConfiguration.class)
@JsonSerialize(as = ImapClientFactoryConfiguration.class)
public interface ImapClientFactoryConfigurationIF {
  HostAndPort hostAndPort();

  AuthType authType();

  @Default
  default EventLoopGroup eventLoopGroup() {
    return new NioEventLoopGroup();
  }

  @Default
  default EventExecutorGroup executor() {
    Logger logger = LoggerFactory.getLogger("imap-executor");
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception on thread {}", t.getName(), e))
        .setNameFormat("imap-executor-%d")
        .build();

    int nThreads = Runtime.getRuntime().availableProcessors() * 2;
    return new DefaultEventExecutorGroup(nThreads, threadFactory);
  }

  @Default
  default Class<? extends Channel> channelClass() {
    return NioSocketChannel.class;
  }

  @Default
  default EpollMode epollMode() {
    return EpollMode.EDGE_TRIGGERED;
  }

  @Default
  default boolean useSsl() {
    return true;
  }

  @Default
  default int noopKeepAliveIntervalSec() {
    return -1;
  }

  @Default
  default int socketTimeoutMs() {
    return 90000;
  }

  @Default
  default int writeBackOffMs() {
    return 100;
  }

  @Default
  default int maxLineLength() {
    return 100000;
  }

  @Default
  default int defaultResponseBufferSize() {
    return 1000;
  }

  @Default
  default int connectTimeoutMillis() {
    return 5000;
  }

  @Default
  default int closeTimeoutSec() {
    return 5;
  }

  @Default
  default int soLinger() {
    return 5;
  }

  @Default
  default boolean tracingEnabled() {
    return false;
  }

  @Default
  default int maxHeaderCount() {
    return 10000;
  }

  enum AuthType {
    PASSWORD,
    XOAUTH2;
  }
}
