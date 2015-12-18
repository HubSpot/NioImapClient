package com.hubspot.imap.protocol.command;

import com.hubspot.imap.utils.GmailUtils;

public class ListCommand extends BaseImapCommand {
  public ListCommand(String reference, String query) {
    super(ImapCommandType.LIST, GmailUtils.quote(reference), GmailUtils.quote(query));
  }
}
