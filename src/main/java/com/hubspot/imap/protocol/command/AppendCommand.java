package com.hubspot.imap.protocol.command;

import static com.hubspot.imap.utils.formats.ImapDateFormat.INTERNALDATE_FORMATTER;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hubspot.imap.protocol.message.MessageFlag;
import com.hubspot.imap.utils.GmailUtils;

public class AppendCommand extends BaseImapCommand {
  private final String folderName;
  private final Set<MessageFlag> flags;
  private final Optional<ZonedDateTime> dateTime;
  private final int size;

  public AppendCommand(String folderName, Set<MessageFlag> flags, Optional<ZonedDateTime> dateTime, int size) {
    super(ImapCommandType.APPEND);
    this.folderName = folderName;
    this.flags = flags;
    this.dateTime = dateTime;
    this.size = size;
  }

  @Override
  public List<String> getArgs() {
    List<String> args = new ArrayList<>();
    args.add(getPrefix());
    args.add(GmailUtils.quote(folderName));

    if(!flags.isEmpty()) {
      args.add(getFlagString(flags));
    }

    if (dateTime.isPresent()) {
      args.add(GmailUtils.quote(dateTime.get().format(INTERNALDATE_FORMATTER)));
    }

    args.add(String.format("{%d}", size));
    return args;
  }

  @Override
  public String commandString() {
    return getArgs().stream().collect(Collectors.joining(" "));
  }
}
