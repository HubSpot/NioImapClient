package com.hubspot.imap.protocol.command;

import com.hubspot.imap.client.FolderOpenMode;
import com.hubspot.imap.utils.GmailUtils;

public class OpenCommand extends BaseImapCommand {
  public OpenCommand(String name, FolderOpenMode openMode) {
    super(openMode == FolderOpenMode.READ ? ImapCommandType.EXAMINE : ImapCommandType.SELECT, GmailUtils.quote(name));
  }
}
