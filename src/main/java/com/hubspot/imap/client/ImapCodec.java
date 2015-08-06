package com.hubspot.imap.client;

import com.hubspot.imap.protocol.command.BaseCommand;
import com.hubspot.imap.protocol.command.CommandType;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.response.ContinuationResponse;
import com.hubspot.imap.protocol.response.events.ExistsEvent;
import com.hubspot.imap.protocol.response.events.ExpungeEvent;
import com.hubspot.imap.protocol.response.events.FetchEvent;
import com.hubspot.imap.protocol.response.events.OpenEvent;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.ListResponse.Builder;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedIntResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedResponseType;
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
    String tag = clientState.getNextTag();
    LOGGER.debug("SEND: {}{}", tag, data);
    out.add(tag + data + "\r\n");
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
    if (msg instanceof ContinuationResponse) {
      out.add(msg);
    } else if (msg instanceof TaggedResponse) {
      TaggedResponse taggedResponse = ((TaggedResponse) msg);
      fireEvents(ctx, taggedResponse);
      switch (clientState.getCurrentCommand().getCommandType()) {
        case LIST:
          taggedResponse = new Builder().fromResponse(taggedResponse);
          break;
        case SELECT:
        case EXAMINE:
          taggedResponse = new OpenResponse.Builder().fromResponse(taggedResponse);

          ctx.fireUserEventTriggered(new OpenEvent(((OpenResponse) taggedResponse)));
          break;
        case FETCH:
          taggedResponse = new FetchResponse.Builder().fromResponse(taggedResponse);
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

  private void fireEvents(ChannelHandlerContext ctx, TaggedResponse response) {
    fireFetchEvents(ctx, response);
    fireMessageNumberEvents(ctx, response);
  }

  private void fireMessageNumberEvents(ChannelHandlerContext ctx, TaggedResponse response) {
    CommandType commandType = clientState.getCurrentCommand().getCommandType();
    if (commandType != CommandType.EXAMINE && commandType != CommandType.SELECT) { // Don't fire these events during folder open, they have different meaning here
      response.getUntagged().stream().filter(r -> r instanceof UntaggedIntResponse).map(i -> ((UntaggedIntResponse) i)).forEach((i) -> {
        if (i.getType() == UntaggedResponseType.EXPUNGE) {
          ctx.fireUserEventTriggered(new ExpungeEvent(i.getValue()));
        } else if (i.getType() == UntaggedResponseType.EXISTS) {
          ctx.fireUserEventTriggered(new ExistsEvent(i.getValue()));
        }
      });
    }
  }

  private void fireFetchEvents(ChannelHandlerContext ctx, TaggedResponse response) {
    Set<ImapMessage> messages = response.getUntagged().stream()
        .filter(m -> m instanceof ImapMessage).map(m -> ((ImapMessage) m))
        .collect(Collectors.toSet());

    FetchEvent event = new FetchEvent(messages);
    ctx.fireUserEventTriggered(event);
  }
}
