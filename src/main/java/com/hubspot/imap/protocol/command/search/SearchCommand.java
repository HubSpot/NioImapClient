package com.hubspot.imap.protocol.command.search;

import com.hubspot.imap.protocol.command.BaseCommand;
import com.hubspot.imap.protocol.command.CommandType;
import com.hubspot.imap.protocol.command.search.keys.SearchKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SearchCommand extends BaseCommand {
  private List<SearchKey> keys;

  public SearchCommand(SearchKey... keys) {
    super(CommandType.SEARCH,
          JOINER.join(Arrays.asList(keys).stream()
                        .map(SearchKey::keyString)
                        .collect(Collectors.toList())));
    this.keys = Arrays.asList(keys);
  }

  public List<SearchKey> getKeys() {
    return keys;
  }
}
