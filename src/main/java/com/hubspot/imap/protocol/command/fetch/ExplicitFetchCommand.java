package com.hubspot.imap.protocol.command.fetch;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.hubspot.imap.protocol.command.BaseImapCommand;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExplicitFetchCommand extends BaseImapCommand {
  private static final Joiner COMMA_JOINER = Joiner.on(",");
  private Set<Long> ids;
  private final List<FetchDataItem> fetchDataItems;

  // The IDs here should be sequence numbers, unless you intend to wrap this command with a UidCommand
  public ExplicitFetchCommand(Set<Long> ids, List<FetchDataItem> fetchDataItems) {
    super(ImapCommandType.FETCH);

    this.ids = ids;
    this.fetchDataItems = fetchDataItems;
  }

  public ExplicitFetchCommand(Set<Long> ids, FetchDataItem fetchItem, FetchDataItem... otherFetchItems) {
    this(ids, Lists.asList(fetchItem, otherFetchItems));
  }

  @Override
  public List<String> getArgs() {
    return Lists.newArrayList(COMMA_JOINER.join(ids), getFetchItems());
  }

  @Override
  public boolean hasArgs() {
    return true;
  }

  private String getFetchItems() {
    if (fetchDataItems.size() == 1) {
      return fetchDataItems.get(0).toString();
    }

    return String.format("(%s)", SPACE_JOINER.join(fetchDataItems.stream().map(FetchDataItem::toString).collect(Collectors.toList())));
  }
}

