package com.hubspot.imap.utils;

import com.hubspot.imap.protocol.command.Command;
import com.hubspot.imap.protocol.command.fetch.StreamingFetchCommand;
import com.hubspot.imap.protocol.command.fetch.UidCommand;

public class CommandUtils {

  public static boolean isStreamingFetch(Command command) {
    return command instanceof StreamingFetchCommand ||
        (command instanceof UidCommand && ((UidCommand) command).getWrappedCommand() instanceof StreamingFetchCommand);
  }
}
