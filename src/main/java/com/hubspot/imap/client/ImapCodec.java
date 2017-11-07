package com.hubspot.imap.client;

import java.util.List;

import org.slf4j.Logger;

import com.hubspot.imap.ImapChannelAttrs;
import com.hubspot.imap.protocol.command.BaseImapCommand;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.response.ContinuationResponse;
import com.hubspot.imap.protocol.response.events.ExistsEvent;
import com.hubspot.imap.protocol.response.events.ExpungeEvent;
import com.hubspot.imap.protocol.response.events.OpenEvent;
import com.hubspot.imap.protocol.response.tagged.FetchResponse;
import com.hubspot.imap.protocol.response.tagged.ListResponse.Builder;
import com.hubspot.imap.protocol.response.tagged.NoopResponse;
import com.hubspot.imap.protocol.response.tagged.OpenResponse;
import com.hubspot.imap.protocol.response.tagged.SearchResponse;
import com.hubspot.imap.protocol.response.tagged.StreamingFetchResponse;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedIntResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedResponseType;
import com.hubspot.imap.utils.CommandUtils;
import com.hubspot.imap.utils.LogUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

public class ImapCodec extends MessageToMessageCodec<Object, BaseImapCommand> {
  private final Logger logger;
  private final ImapClientState clientState;

  public ImapCodec(ImapClientState clientState) {
    this.logger = LogUtils.loggerWithName(ImapCodec.class, clientState.getClientName());
    this.clientState = clientState;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, BaseImapCommand msg, List<Object> out) throws Exception {
    String data = msg.commandString();

    if (msg.getRequiresTag()) {
      String tag = clientState.getNextTag();

      trace(ctx, tag, data);

      out.add(tag + data + "\r\n");
    } else {
      trace(ctx, "", data);

      out.add(data + "\r\n");
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
    if (msg instanceof ContinuationResponse) {
      out.add(msg);
    } else if (msg instanceof TaggedResponse) {
      TaggedResponse taggedResponse = ((TaggedResponse) msg);
      fireEvents(ctx, taggedResponse);
      switch (clientState.getCurrentCommand().getCommandType()) {
        case SEARCH:
          taggedResponse = new SearchResponse.Builder().fromResponse(taggedResponse);
          break;
        case LIST:
          taggedResponse = new Builder().fromResponse(taggedResponse);
          break;
        case SELECT:
        case EXAMINE:
          taggedResponse = new OpenResponse.Builder().fromResponse(taggedResponse);

          ctx.fireUserEventTriggered(new OpenEvent(((OpenResponse) taggedResponse)));
          break;
        case FETCH:
          if (CommandUtils.isStreamingFetch(clientState.getCurrentCommand())) {
            taggedResponse = new StreamingFetchResponse.Builder().fromResponse(taggedResponse);
          } else {
            taggedResponse = new FetchResponse.Builder().fromResponse(taggedResponse);
          }

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
    fireMessageNumberEvents(ctx, response);
  }

  private void fireMessageNumberEvents(ChannelHandlerContext ctx, TaggedResponse response) {
    ImapCommandType imapCommandType = clientState.getCurrentCommand().getCommandType();
    if (imapCommandType != ImapCommandType.EXAMINE && imapCommandType != ImapCommandType.SELECT) { // Don't fire these events during folder open, they have different meaning here
      response.getUntagged().stream().filter(r -> r instanceof UntaggedIntResponse).map(i -> ((UntaggedIntResponse) i)).forEach((i) -> {
        if (i.getType() == UntaggedResponseType.EXPUNGE) {
          ctx.fireUserEventTriggered(new ExpungeEvent(i.getValue()));
        } else if (i.getType() == UntaggedResponseType.EXISTS) {
          ctx.fireUserEventTriggered(new ExistsEvent(i.getValue()));
        }
      });
    }
  }

  private void trace(ChannelHandlerContext ctx, String tag, String data) {
    if (ctx.channel().attr(ImapChannelAttrs.CONFIGURATION).get().tracingEnabled()) {
      logger.info("SEND: {}{}", tag, data);
    }
  }
}
