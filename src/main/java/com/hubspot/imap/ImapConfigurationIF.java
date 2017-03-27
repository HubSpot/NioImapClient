package com.hubspot.imap;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.HostAndPort;

import io.netty.channel.epoll.EpollMode;

@Immutable
@Style(
    typeAbstract = {"*IF"},
    typeImmutable = "*"
)
@JsonDeserialize(as = ImapConfiguration.class)
@JsonSerialize(as = ImapConfiguration.class)
public interface ImapConfigurationIF {
  HostAndPort hostAndPort();

  AuthType authType();

  @Default
  default EpollMode epollMode() {
    return EpollMode.EDGE_TRIGGERED;
  }

  @Default
  default boolean useEpoll() {
    return false;
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
  default int numEventLoopThreads() {
    return 0;
  }

  @Default
  default int numExecutorThreads() {
    return Runtime.getRuntime().availableProcessors() * 2;
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
