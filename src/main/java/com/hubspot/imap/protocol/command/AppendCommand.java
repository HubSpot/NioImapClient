package com.hubspot.imap.protocol.command;

import static com.hubspot.imap.utils.formats.ImapDateFormat.IMAP_FULL_DATE_FORMAT;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.UnexpectedAppendResponseException;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.message.MessageFlag;
import com.hubspot.imap.protocol.message.UnfetchedFieldException;
import com.hubspot.imap.protocol.response.ContinuationResponse;
import com.hubspot.imap.protocol.response.ImapResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import com.hubspot.imap.utils.GmailUtils;
import com.hubspot.imap.utils.ImapMessageWriterUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AppendCommand extends ContinuableCommand<TaggedResponse> {

  private final String folderName;
  private final Set<MessageFlag> flags;
  private final Optional<ZonedDateTime> dateTime;
  private final StringLiteralCommand stringLiteralCommand;

  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public AppendCommand(
    ImapClient imapClient,
    String folderName,
    Set<MessageFlag> flags,
    Optional<ZonedDateTime> dateTime,
    ImapMessage message
  ) throws UnfetchedFieldException, IOException {
    super(imapClient, ImapCommandType.APPEND);
    this.folderName = folderName;
    this.flags = flags;
    this.dateTime = dateTime;
    this.stringLiteralCommand =
      new StringLiteralCommand(
        ImapMessageWriterUtils.messageBodyToString(message),
        Charset.forName(message.getBody().getCharset())
      );
  }

  public StringLiteralCommand getStringLiteralCommand() {
    return stringLiteralCommand;
  }

  @Override
  public List<String> getArgs() {
    List<String> args = new ArrayList<>();
    args.add(getPrefix());
    args.add(GmailUtils.quote(folderName));

    if (!flags.isEmpty()) {
      args.add(getFlagString(flags));
    }

    if (dateTime.isPresent()) {
      args.add(GmailUtils.quote(dateTime.get().format(IMAP_FULL_DATE_FORMAT)));
    }

    args.add(String.format("{%d}", stringLiteralCommand.getSize()));
    return args;
  }

  @Override
  public String commandString() {
    return getArgs().stream().collect(Collectors.joining(" "));
  }

  @Override
  public CompletableFuture<TaggedResponse> continueAfterResponse(
    ImapResponse imapResponse,
    Throwable throwable
  ) {
    if (throwable != null) {
      throw new UnexpectedAppendResponseException(throwable);
    }

    if (!(imapResponse instanceof ContinuationResponse)) {
      throw new UnexpectedAppendResponseException(imapResponse);
    }

    return imapClient.send(getStringLiteralCommand());
  }
}
