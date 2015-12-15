package com.hubspot.imap.protocol.command.atoms;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;

public abstract class BaseImapAtom implements ImapAtom {
  protected static final Joiner SPACE_JOINER = Joiner.on(" ").skipNulls();

  protected final List<String> args;

  public BaseImapAtom(String... args) {
    this.args = Lists.newArrayList(args);
  }

  public String imapString() {
    if (hasArgs()) {
      return String.format("%s %s", getPrefix(), SPACE_JOINER.join(getArgs())).trim();
    } else {
      return getPrefix();
    }
  }

  public abstract String getPrefix();

  public List<String> getArgs() {
    return args;
  }

  public boolean hasArgs() {
    return args.size() > 0;
  }

  public String toString() {
    return imapString();
  }
}
