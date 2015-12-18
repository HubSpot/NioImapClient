package com.hubspot.imap.protocol.command;

import com.google.common.collect.Lists;
import com.hubspot.imap.protocol.message.MessageFlag;

import java.util.List;
import java.util.stream.Collectors;

public class StoreCommand extends BaseImapCommand {

  public enum StoreAction {
    SET_FLAGS("FLAGS"),
    ADD_FLAGS("+FLAGS"),
    REMOVE_FLAGS("-FLAGS");

    private String string;

    StoreAction(String string) {
      this.string = string;
    }

    public String getString() {
      return string;
    }
  }

  protected final StoreAction action;
  private final long startId;
  private final long stopId;

  private List<MessageFlag> flags;

  public StoreCommand(StoreAction action, long startId, long stopId, MessageFlag... args) {
    super(ImapCommandType.STORE);

    this.action = action;
    this.startId = startId;
    this.stopId = stopId;

    this.flags = Lists.newArrayList(args);
  }

  @Override
  public List<String> getArgs() {
    return Lists.newArrayList(getRange(), getAction(), getFlagString());
  }

  @Override
  public boolean hasArgs() {
    return flags.size() > 0;
  }

  protected String getAction() {
    return action.getString();
  }

  private String getRange() {
    return String.valueOf(startId) + ":" + String.valueOf(stopId);
  }

  private String getFlagString() {
    return "(" + SPACE_JOINER.join(flags.stream().map(MessageFlag::getString).collect(Collectors.toList())) + ")";
  }
}
