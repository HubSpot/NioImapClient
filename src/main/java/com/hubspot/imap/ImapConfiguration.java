package com.hubspot.imap;

import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;

public interface ImapConfiguration {

  HostAndPort getHostAndPort();
  AuthType getAuthType();

  enum AuthType {
    PASSWORD,
    XOAUTH2;
  }

  class Builder implements ImapConfiguration {
    public HostAndPort hostAndPort;
    public AuthType authType;

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

    public ImapConfiguration build() {
      return this;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("hostAndPort", hostAndPort)
          .add("authType", authType)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Builder builder = (Builder) o;
      return Objects.equal(getHostAndPort(), builder.getHostAndPort()) &&
          Objects.equal(getAuthType(), builder.getAuthType());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getHostAndPort(), getAuthType());
    }
  }
}
