package com.hubspot.imap.protocol.command.fetch;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hubspot.imap.protocol.command.BaseImapCommand;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FetchCommand extends BaseImapCommand {
  private static final String STAR = "*";

  private final long startId;
  private final Optional<Long> stopId;
  private final List<FetchDataItem> fetchDataItems;

  // The IDs here should be sequence numbers, unless you intend to wrap this command with a UidCommand
  public FetchCommand(long startId, Optional<Long> stopId, List<FetchDataItem> fetchDataItems) {
    super(ImapCommandType.FETCH);
    Preconditions.checkState(startId >= 1, "Start ID must be 1 or greater.");

    this.startId = startId;
    this.stopId = stopId;
    this.fetchDataItems = fetchDataItems;
  }

  public FetchCommand(long startId, Optional<Long> stopId, FetchDataItem fetchItem, FetchDataItem... otherFetchItems) {
    this(startId, stopId, Lists.asList(fetchItem, otherFetchItems));
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
        SPACE_JOINER.join(fetchDataItems.stream().map(FetchDataItem::toString).collect(Collectors.toList())));
  }

  public long getStartId() {
    return startId;
  }

  public Optional<Long> getStopId() {
    return stopId;
  }
}
