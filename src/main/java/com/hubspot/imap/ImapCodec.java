package com.hubspot.imap;

import com.hubspot.imap.imap.Command;
import com.hubspot.imap.imap.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ImapCodec extends MessageToMessageCodec<String, Command> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapCodec.class);

  @Override
  protected void encode(ChannelHandlerContext ctx, Command msg, List<Object> out) throws Exception {
    String data = msg.toString();
    LOGGER.debug("IMAP SEND: {}", data);
    out.add(data + "\r\n");
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
    Response response = Response.parse(msg);
    LOGGER.debug("IMAP RECEIVE: {}", response);
    out.add(response);
  }
}
