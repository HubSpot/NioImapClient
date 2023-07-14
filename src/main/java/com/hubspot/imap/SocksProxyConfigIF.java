package com.hubspot.imap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.HostAndPort;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(typeAbstract = { "*IF" }, typeImmutable = "*")
@JsonDeserialize(as = SocksProxyConfig.class)
@JsonSerialize(as = SocksProxyConfig.class)
public interface SocksProxyConfigIF {
  HostAndPort proxyHost();
}
