package com.hubspot.imap.client;

import com.hubspot.imap.imap.command.BaseCommand;
import com.hubspot.imap.imap.command.fetch.FetchCommand;
import com.hubspot.imap.imap.message.ImapMessage;
import com.hubspot.imap.imap.response.ContinuationResponse;
import com.hubspot.imap.imap.response.tagged.FetchResponse;
import com.hubspot.imap.imap.response.tagged.ListResponse.Builder;
import com.hubspot.imap.imap.response.tagged.NoopResponse;
import com.hubspot.imap.imap.response.tagged.OpenResponse;
import com.hubspot.imap.imap.response.tagged.TaggedResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImapCodec extends MessageToMessageCodec<Object, BaseCommand> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapCodec.class);

  private final ImapClientState clientState;

  public ImapCodec(ImapClientState clientState) {
    this.clientState = clientState;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, BaseCommand msg, List<Object> out) throws Exception {
    String data = msg.commandString();
    LOGGER.info("IMAP SEND: {}", data);
    out.add(clientState.getNextTag() + data + "\r\n");
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
    if (msg instanceof ContinuationResponse) {
      out.add(msg);
    } else if (msg instanceof TaggedResponse) {
      TaggedResponse taggedResponse = ((TaggedResponse) msg);
      switch (clientState.getCurrentCommand().getCommandType()) {
        case LIST:
          taggedResponse = new Builder().fromResponse(taggedResponse);
          break;
        case SELECT:
        case EXAMINE:
          taggedResponse = new OpenResponse.Builder().fromResponse(taggedResponse);
          break;
        case FETCH:
          FetchCommand fetchCommand = ((FetchCommand) clientState.getCurrentCommand());

          Set<ImapMessage> allMessages = taggedResponse.getUntagged().stream()
              .filter(u -> u instanceof ImapMessage).map(u -> ((ImapMessage) u))
              .collect(Collectors.toSet());
          Set<ImapMessage> messages = filterFetchedMessages(fetchCommand, allMessages);

          taggedResponse = new FetchResponse.Builder().fromResponse(taggedResponse, messages);
          break;
        case NOOP:
          taggedResponse = new NoopResponse.Builder().fromResponse(taggedResponse);
          break;
        default:
          break;
      }

      out.add(taggedResponse);
    }
  }

  private Set<ImapMessage> filterFetchedMessages(FetchCommand fetchCommand, Set<ImapMessage> messages) {
    return messages.stream().filter(m -> {
      if (fetchCommand.getStopId().isPresent()) {
        return m.getMessageNumber() >= fetchCommand.getStartId() &&
            m.getMessageNumber() <= fetchCommand.getStopId().get();
      } else {
        return m.getMessageNumber() >= fetchCommand.getStartId();
      }
    }).collect(Collectors.toSet());
  }
}
