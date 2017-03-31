package com.hubspot.imap.protocol.command;

import com.hubspot.imap.protocol.command.option.ReturnOption;
import com.hubspot.imap.protocol.command.option.SelectOption;
import com.hubspot.imap.utils.GmailUtils;

public class ListCommand extends BaseImapCommand {
  public ListCommand(String reference, String query) {
    super(ImapCommandType.LIST, GmailUtils.quote(reference), GmailUtils.quote(query));
  }

  public ListCommand(String reference, String query, ReturnOption returnOption) {
    super(ImapCommandType.LIST, GmailUtils.quote(reference), GmailUtils.quote(query), "RETURN", "(" + returnOption.getName() + ")");
  }

  public ListCommand(String reference, String query, SelectOption selectOption) {
    super(ImapCommandType.LIST, "(" + selectOption.getName() + ")", GmailUtils.quote(reference), GmailUtils.quote(query));
  }
}
