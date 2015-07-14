package com.hubspot.imap.imap.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

public class BaseCommand implements Command {
  private static final Joiner JOINER = Joiner.on(" ").skipNulls();

  protected final CommandType type;
  protected final String tag;
  protected final List<String> args;

  public BaseCommand(CommandType type, long tag, String... args) {
    this.type = type;
    this.tag = "A" + String.valueOf(tag);
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
    return String.format("%s %s", tag, type.name());
  }

  public CommandType getType() {
    return type;
  }

  public String getTag() {
    return tag;
  }

  @Override
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
