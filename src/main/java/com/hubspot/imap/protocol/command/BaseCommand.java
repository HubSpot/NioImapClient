package com.hubspot.imap.protocol.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

public class BaseCommand implements Command {
  protected static final Joiner JOINER = Joiner.on(" ").skipNulls();

  protected final CommandType type;
  protected final List<String> args;

  public BaseCommand(CommandType type, String... args) {
    this.type = type;
    this.args = Lists.newArrayList(args);
  }

  public String commandString() {
    if (hasArgs()) {
      return String.format("%s %s", getCommandPrefix(), JOINER.join(getArgs())).trim();
    } else {
      return getCommandPrefix();
    }
  }

  public String getCommandPrefix() {
    return type.name();
  }

  public CommandType getCommandType() {
    return type;
  }

  public List<String> getArgs() {
    return args;
  }

  public boolean hasArgs() {
    return args.size() > 0;
  }
}
