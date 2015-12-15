package com.hubspot.imap.protocol.command;

import com.hubspot.imap.utils.GmailUtils;

public class OpenCommand extends BaseImapCommand {
  public OpenCommand(String name, boolean readOnly) {
    super(readOnly ? ImapCommandType.EXAMINE : ImapCommandType.SELECT, GmailUtils.quote(name));
  }
}
