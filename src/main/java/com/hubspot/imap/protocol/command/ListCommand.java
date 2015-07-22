package com.hubspot.imap.protocol.command;

import com.hubspot.imap.utils.GmailUtils;

public class ListCommand extends BaseCommand {
  public ListCommand(String reference, String query) {
    super(CommandType.LIST, GmailUtils.quote(reference), GmailUtils.quote(query));
  }
}
