package com.hubspot.imap;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style.ImplementationVisibility;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@Value.Style(
    typeAbstract = {"*IF"},
    typeImmutable = "*",
    visibility = ImplementationVisibility.SAME
)
@JsonSerialize(as = TestServerConfig.class)
@JsonDeserialize(as = TestServerConfig.class)
public interface TestServerConfigIF {
  String host();
  String user();

  String password();

  ImapClientConfiguration imapConfiguration();

  @Default
  default int port() {
    return 993;
  }

  @Default
  default String primaryFolder() {
    return "INBOX";
  }
}
