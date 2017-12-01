package com.hubspot.imap.protocol.capabilities;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;

public enum StandardCapabilities implements Capability {
  ACL,
  ANNOTATE_EXPERIMENT_1,
  APPENDLIMIT,
  AUTH,
  BINARY,
  CATENATE,
  CHILDREN,
  COMPRESS_DEFLATE("COMPRESS=DEFLATE"),
  CONDSTORE,
  CONTEXT_SEARCH("CONTEXT=SEARCH"),
  CONTEXT_SORT("CONTEXT=SORT"),
  CONVERT,
  CREATE_SPECIAL_USE,
  ENABLE,
  ESEARCH,
  ESORT,
  FILTERS,
  I18NLEVEL_1("I18NLEVEL=1"),
  I18NLEVEL_2("I18NLEVEL=2"),
  ID,
  IDLE,
  IMAPSIEVE("IMAPSIEVE="),
  LANGUAGE,
  LIST_EXTENDED,
  LIST_STATUS,
  LITERAL_PLUS("LITERAL+"),
  LITERAL("LITERAL-"),
  LOGIN_REFERRALS,
  LOGINDISABLED,
  MAILBOX_REFERRALS,
  METADATA,
  METADATA_SERVER,
  MOVE,
  MULTIAPPEND,
  MULTISEARCH,
  NAMESPACE,
  NOTIFY,
  QRESYNC,
  QUOTA,
  RIGHTS("RIGHTS="),
  SASL_IR,
  SEARCH_FUZZY("SEARCH=FUZZY"),
  SEARCHRES,
  SORT,
  SORT_DISPLAY("SORT=DISPLAY"),
  SPECIAL_USE,
  STARTTLS,
  THREAD,
  UIDPLUS,
  UNSELECT,
  URLFETCH_BINARY("URLFETCH=BINARY"),
  URL_PARTIAL,
  URLAUTH,
  UTF8_ACCEPT("UTF8=ACCEPT"),
  UTF8_ONLY("UTF8=ONLY"),
  WITHIN,
  ;

  private final String capability;

  StandardCapabilities() {
    this(Optional.empty());
  }

  StandardCapabilities(String capability) {
    this(Optional.of(capability));
  }

  StandardCapabilities(Optional<String> capability) {
    this.capability = capability.orElse(name().replaceAll("_", "-"));
  }

  @Override
  public String getCapability() {
    return capability;
  }

  private static final Map<String, StandardCapabilities> INDEX = Maps.uniqueIndex(Arrays.asList(StandardCapabilities.values()), StandardCapabilities::getCapability);

  public static Optional<Capability> fromString(String capability) {
    return Optional.ofNullable(INDEX.get(capability));
  }
}
