package com.hubspot.imap.imap.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

public class BaseCommand implements Command {
  private static final Joiner JOINER = Joiner.on(" ").skipNulls();

  protected final CommandType type;
  protected final String id;
  protected final List<String> args;

  public BaseCommand(CommandType type, int id, String... args) {
    this.type = type;
    this.id = "A" + String.valueOf(id);
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
    return String.format("%s %s", id, type.name());
  }

  public CommandType getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public List<String> getArgs() {
    return args;
  }

  public boolean hasArgs() {
    return args.size() > 0;
  }
}
