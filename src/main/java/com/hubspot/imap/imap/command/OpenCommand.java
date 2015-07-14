package com.hubspot.imap.imap.command;

import com.hubspot.imap.utils.GmailUtils;

public class OpenCommand extends BaseCommand {
  public OpenCommand(long tag, String name, boolean readOnly) {
    super(readOnly ? CommandType.EXAMINE : CommandType.SELECT, tag, GmailUtils.quote(name));
  }
}
