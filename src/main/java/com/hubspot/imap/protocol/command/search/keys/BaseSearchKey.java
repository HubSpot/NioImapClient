package com.hubspot.imap.protocol.command.search.keys;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.List;

public class BaseSearchKey implements SearchKey {
  private static final Joiner JOINER = Joiner.on(" ").skipNulls();
  private final SearchKeyType keyType;
  private final List<String> args;

  public BaseSearchKey(SearchKeyType type, String... args) {
    this.keyType = type;
    this.args = Arrays.asList(args);
  }

  public String keyString() {
    if (hasArgs()) {
      return String.format("%s %s", getKeyPrefix(), JOINER.join(getArgs())).trim();
    } else {
      return getKeyPrefix();
    }
  }

  public String getKeyPrefix() {
    return keyType.toString();
  }

  public SearchKeyType getKeyType() {
    return keyType;
  }

  public List<String> getArgs() {
    return args;
  }

  public boolean hasArgs() {
    return args.size() > 0;
  }
}
