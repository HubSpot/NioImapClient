package com.hubspot.imap;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.HostAndPort;

@Value.Immutable
@Value.Style(
    typeAbstract = {"*IF"},
    typeImmutable = "*"
)
@JsonDeserialize(as = ProxyConfig.class)
@JsonSerialize(as = ProxyConfig.class)
public interface ProxyConfigIF {
  HostAndPort proxyHost();

  @Value.Default
  default Optional<String> proxyLocalIpAddress() {
    return Optional.empty();
  }

  @Value.Default
  default Optional<String> proxyPublicIpAddress() {
    return Optional.empty();
  }
}
