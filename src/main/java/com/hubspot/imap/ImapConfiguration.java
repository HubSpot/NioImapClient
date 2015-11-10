package com.hubspot.imap;

import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;
import org.apache.commons.lang.SystemUtils;

public interface ImapConfiguration {

  HostAndPort getHostAndPort();
  AuthType getAuthType();

  boolean getUseEpoll();

  int getNoopKeepAliveIntervalSec();
  int getSocketTimeoutMs();

  int getWriteBackOffMs();

  int getNumEventLoopThreads();
  int getNumExecutorThreads();

  int getMaxLineLength();
  int getDefaultResponseBufferSize();

  int getCloseTimeoutSec();

  enum AuthType {
    PASSWORD,
    XOAUTH2;
  }

  class Builder implements ImapConfiguration {
    private HostAndPort hostAndPort;
    private AuthType authType;

    private boolean useEpoll;

    private int noopKeepAliveIntervalSec;
    private int socketTimeoutMs = 1000;

    private int writeBackOffMs = 10;

    private int numEventLoopThreads = 0;
    private int numExecutorThreads = 16;

    private int maxLineLength = 100000;
    private int defaultResponseBufferSize = 1000;

    private int closeTimeoutSec = 30;

    public HostAndPort getHostAndPort() {
      return this.hostAndPort;
    }

    public Builder setHostAndPort(HostAndPort hostAndPort) {
      this.hostAndPort = hostAndPort;
      return this;
    }

    public AuthType getAuthType() {
      return this.authType;
    }

    public Builder setAuthType(AuthType authType) {
      this.authType = authType;
      return this;
    }

    public int getNoopKeepAliveIntervalSec() {
      return this.noopKeepAliveIntervalSec;
    }

    public Builder setNoopKeepAliveIntervalSec(int noopKeepAliveIntervalSec) {
      this.noopKeepAliveIntervalSec = noopKeepAliveIntervalSec;
      return this;
    }

    public boolean getUseEpoll() {
      return this.useEpoll && SystemUtils.IS_OS_LINUX;
    }

    public Builder setUseEpoll(boolean useEpoll) {
      this.useEpoll = useEpoll;
      return this;
    }

    public int getSocketTimeoutMs() {
      return this.socketTimeoutMs;
    }

    public Builder setSocketTimeoutMs(int socketTimeoutMs) {
      this.socketTimeoutMs = socketTimeoutMs;
      return this;
    }

    public int getWriteBackOffMs() {
      return this.writeBackOffMs;
    }

    public Builder setWriteBackOffMs(int writeBackOffMs) {
      this.writeBackOffMs = writeBackOffMs;
      return this;
    }

    public int getNumEventLoopThreads() {
      return this.numEventLoopThreads;
    }

    public Builder setNumEventLoopThreads(int numEventLoopThreads) {
      this.numEventLoopThreads = numEventLoopThreads;
      return this;
    }

    public int getNumExecutorThreads() {
      return this.numExecutorThreads;
    }

    public Builder setNumExecutorThreads(int numExecutorThreads) {
      this.numExecutorThreads = numExecutorThreads;
      return this;
    }

    public int getMaxLineLength() {
      return this.maxLineLength;
    }

    public Builder setMaxLineLength(int maxLineLength) {
      this.maxLineLength = maxLineLength;
      return this;
    }

    public int getDefaultResponseBufferSize() {
      return this.defaultResponseBufferSize;
    }

    public Builder setDefaultResponseBufferSize(int defaultResponseBufferSize) {
      this.defaultResponseBufferSize = defaultResponseBufferSize;
      return this;
    }

    public int getCloseTimeoutSec() {
      return this.closeTimeoutSec;
    }

    public Builder setCloseTimeoutSec(int closeTimeoutSec) {
      this.closeTimeoutSec = closeTimeoutSec;
      return this;
    }

    public ImapConfiguration build() {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Builder builder = (Builder) o;
      return getUseEpoll() == builder.getUseEpoll() &&
          getNoopKeepAliveIntervalSec() == builder.getNoopKeepAliveIntervalSec() &&
          getSocketTimeoutMs() == builder.getSocketTimeoutMs() &&
          getWriteBackOffMs() == builder.getWriteBackOffMs() &&
          getNumEventLoopThreads() == builder.getNumEventLoopThreads() &&
          getNumExecutorThreads() == builder.getNumExecutorThreads() &&
          getMaxLineLength() == builder.getMaxLineLength() &&
          getDefaultResponseBufferSize() == builder.getDefaultResponseBufferSize() &&
          getCloseTimeoutSec() == builder.getCloseTimeoutSec() &&
          Objects.equal(getHostAndPort(), builder.getHostAndPort()) &&
          getAuthType() == builder.getAuthType();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getHostAndPort(), getAuthType(), getUseEpoll(), getNoopKeepAliveIntervalSec(), getSocketTimeoutMs(), getWriteBackOffMs(), getNumEventLoopThreads(), getNumExecutorThreads(), getMaxLineLength(), getDefaultResponseBufferSize(), getCloseTimeoutSec());
    }
  }
}
