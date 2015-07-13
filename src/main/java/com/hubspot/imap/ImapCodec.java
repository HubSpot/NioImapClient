package com.hubspot.imap;

import com.hubspot.imap.imap.command.BaseCommand;
import com.hubspot.imap.imap.response.tagged.TaggedResponse;
import com.hubspot.imap.imap.response.tagged.ListResponse.Builder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ImapCodec extends MessageToMessageCodec<TaggedResponse, BaseCommand> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapCodec.class);

  private final ImapClient client;

  public ImapCodec(ImapClient client) {
    this.client = client;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, BaseCommand msg, List<Object> out) throws Exception {
    String data = msg.commandString();
    LOGGER.debug("IMAP SEND: {}", data);
    out.add(data + "\r\n");
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, TaggedResponse msg, List<Object> out) throws Exception {
    TaggedResponse taggedResponse = msg;
    switch (client.getCurrentCommand().getCommandType()) {
      case LIST:
        taggedResponse = new Builder().fromResponse(msg, client);
        break;
    }

    out.add(taggedResponse);
  }
}
