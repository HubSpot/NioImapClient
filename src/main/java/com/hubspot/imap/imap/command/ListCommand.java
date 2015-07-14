package com.hubspot.imap.imap.command;

import com.hubspot.imap.utils.GmailUtils;

public class ListCommand extends BaseCommand {
  public ListCommand(int tag, String reference, String query) {
    super(CommandType.LIST, tag, GmailUtils.quote(reference), GmailUtils.quote(query));
  }
}
