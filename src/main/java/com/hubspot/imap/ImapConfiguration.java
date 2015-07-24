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
  int getReconnectBackoffMs();

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
    private int reconnectBackoffMs = -1;

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

    public int getReconnectBackoffMs() {
      return this.reconnectBackoffMs;
    }

    public Builder setReconnectBackoffMs(int reconnectBackoffMs) {
      this.reconnectBackoffMs = reconnectBackoffMs;
      return this;
    }

    public ImapConfiguration build() {
      return this;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("hostAndPort", hostAndPort)
          .add("authType", authType)
          .add("useEpoll", useEpoll)
          .add("noopKeepAliveIntervalSec", noopKeepAliveIntervalSec)
          .add("socketTimeoutMs", socketTimeoutMs)
          .add("reconnectBackoffMs", reconnectBackoffMs)
          .toString();
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
      return Objects.equal(getUseEpoll(), builder.getUseEpoll()) &&
          Objects.equal(getNoopKeepAliveIntervalSec(), builder.getNoopKeepAliveIntervalSec()) &&
          Objects.equal(getSocketTimeoutMs(), builder.getSocketTimeoutMs()) &&
          Objects.equal(getReconnectBackoffMs(), builder.getReconnectBackoffMs()) &&
          Objects.equal(getHostAndPort(), builder.getHostAndPort()) &&
          Objects.equal(getAuthType(), builder.getAuthType());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getHostAndPort(), getAuthType(), getUseEpoll(), getNoopKeepAliveIntervalSec(), getSocketTimeoutMs(), getReconnectBackoffMs());
    }
  }
}
