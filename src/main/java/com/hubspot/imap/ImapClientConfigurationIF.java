package com.hubspot.imap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.HostAndPort;
import com.hubspot.imap.protocol.capabilities.AuthMechanism;
import com.hubspot.imap.utils.ConfigDefaults;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.TrustManagerFactory;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(typeAbstract = { "*IF" }, typeImmutable = "*")
@JsonDeserialize(as = ImapClientConfiguration.class)
@JsonSerialize(as = ImapClientConfiguration.class)
public interface ImapClientConfigurationIF {
  HostAndPort hostAndPort();

  /**
   * @deprecated please use {@link #allowedAuthMechanisms()} instead.
   */
  @Derived
  @Deprecated
  default AuthMechanism authType() {
    return allowedAuthMechanisms().size() > 0
      ? allowedAuthMechanisms().get(0)
      : AuthMechanism.UNKNOWN;
  }

  @Default
  default boolean useSsl() {
    return true;
  }

  @Default
  default Set<String> sslProtocols() {
    return Set.of();
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

  @Default
  default Optional<TrustManagerFactory> trustManagerFactory() {
    return Optional.empty();
  }

  @Default
  default Optional<ProxyConfig> proxyConfig() {
    return Optional.empty();
  }

  @Default
  default Optional<SocksProxyConfig> socksProxyConfig() {
    return Optional.empty();
  }

  @Default
  default List<AuthMechanism> allowedAuthMechanisms() {
    return ConfigDefaults.DEFAULT_ALLOWED_AUTH_MECHANISMS;
  }
}
