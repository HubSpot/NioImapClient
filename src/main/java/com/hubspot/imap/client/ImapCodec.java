package com.hubspot.imap.client;

import com.hubspot.imap.imap.command.BaseCommand;
import com.hubspot.imap.imap.response.ContinuationResponse;
import com.hubspot.imap.imap.response.tagged.ListResponse.Builder;
import com.hubspot.imap.imap.response.tagged.NoopResponse;
import com.hubspot.imap.imap.response.tagged.OpenResponse;
import com.hubspot.imap.imap.response.tagged.TaggedResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ImapCodec extends MessageToMessageCodec<Object, BaseCommand> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapCodec.class);

  private final ImapClientState clientState;

  public ImapCodec(ImapClientState clientState) {
    this.clientState = clientState;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, BaseCommand msg, List<Object> out) throws Exception {
    String data = msg.commandString();
    LOGGER.debug("IMAP SEND: {}", data);
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
        case NOOP:
          taggedResponse = new NoopResponse.Builder().fromResponse(taggedResponse);
          break;
      }

      out.add(taggedResponse);
    }
  }
}
