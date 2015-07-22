package com.hubspot.imap.protocol.command.fetch;

import com.google.seventeen.common.base.Joiner;
import com.google.seventeen.common.collect.Lists;
import com.hubspot.imap.protocol.command.BaseCommand;
import com.hubspot.imap.protocol.command.CommandType;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FetchCommand extends BaseCommand {
  private static final Joiner JOINER = Joiner.on(" ").skipNulls();
  private static final String STAR = "*";

  private final long startId;
  private final Optional<Long> stopId;
  private final List<FetchDataItem> fetchDataItems;

  // The IDs here should be sequence numbers, unless you intend to wrap this command with a UidCommand
  public FetchCommand(long startId, Optional<Long> stopId, FetchDataItem... fetchDataItems) {
    super(CommandType.FETCH);
    this.startId = startId;
    this.stopId = stopId;
    this.fetchDataItems = Lists.newArrayList(fetchDataItems);
  }

  @Override
  public List<String> getArgs() {
    return Lists.newArrayList(getIdRange(), getFetchItems());
  }

  @Override
  public boolean hasArgs() {
    return true;
  }

  private String getIdRange() {
    String stopIdString = stopId.map(String::valueOf).orElse(STAR);
    return String.format("%d:%s", startId, stopIdString);
  }

  private String getFetchItems() {
    if (fetchDataItems.size() == 1) {
      return fetchDataItems.get(0).toString();
    }

    return String.format("(%s)",
        JOINER.join(fetchDataItems.stream().map(FetchDataItem::toString).collect(Collectors.toList())));
  }

  public long getStartId() {
    return startId;
  }

  public Optional<Long> getStopId() {
    return stopId;
  }
}
