package com.hubspot.imap.imap;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

public class Command {
  private static final Joiner JOINER = Joiner.on(" ").skipNulls();

  private final CommandType type;
  private final String id;
  private final List<String> args;

  public Command(CommandType type, int id, String... args) {
    this.type = type;
    this.id = "A" + String.valueOf(id);
    this.args = Lists.newArrayList(args);
  }

  public String toString() {
    return String.format("%s %s %s", id, type.name(), JOINER.join(args));
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
}
