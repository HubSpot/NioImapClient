package com.hubspot.imap.utils;

import com.hubspot.imap.protocol.command.ImapCommand;
import com.hubspot.imap.protocol.command.fetch.StreamingFetchCommand;
import com.hubspot.imap.protocol.command.fetch.UidCommand;

public class CommandUtils {

  public static boolean isStreamingFetch(ImapCommand imapCommand) {
    return imapCommand instanceof StreamingFetchCommand ||
        (imapCommand instanceof UidCommand && ((UidCommand) imapCommand).getWrappedCommand() instanceof StreamingFetchCommand);
  }
}
