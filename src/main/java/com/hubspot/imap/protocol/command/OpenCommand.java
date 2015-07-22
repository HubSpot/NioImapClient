package com.hubspot.imap.protocol.command;

import com.hubspot.imap.utils.GmailUtils;

public class OpenCommand extends BaseCommand {
  public OpenCommand(String name, boolean readOnly) {
    super(readOnly ? CommandType.EXAMINE : CommandType.SELECT, GmailUtils.quote(name));
  }
}
