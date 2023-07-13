package com.hubspot.imap.protocol.command.search;

import com.hubspot.imap.protocol.command.BaseImapCommand;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.search.keys.SearchKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SearchCommand extends BaseImapCommand {

  private List<SearchKey> keys;

  public SearchCommand(SearchKey... keys) {
    super(ImapCommandType.SEARCH, keysAsString(keys));
    this.keys = Arrays.asList(keys);
  }

  private static String keysAsString(SearchKey[] keys) {
    return SPACE_JOINER.join(
      Arrays.asList(keys).stream().map(SearchKey::keyString).collect(Collectors.toList())
    );
  }

  public List<SearchKey> getKeys() {
    return keys;
  }
}
