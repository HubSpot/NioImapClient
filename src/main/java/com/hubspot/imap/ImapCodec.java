package com.hubspot.imap;

import com.hubspot.imap.imap.command.BaseCommand;
import com.hubspot.imap.imap.response.ContinuationResponse;
import com.hubspot.imap.imap.response.RawResponse;
import com.hubspot.imap.imap.response.BaseResponse;
import com.hubspot.imap.imap.response.Response;
import com.hubspot.imap.imap.response.Response.ResponseType;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Sharable
public class ImapCodec extends MessageToMessageCodec<RawResponse, BaseCommand> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapCodec.class);

  @Override
  protected void encode(ChannelHandlerContext ctx, BaseCommand msg, List<Object> out) throws Exception {
    String data = msg.commandString();
    LOGGER.debug("IMAP SEND: {}", data);
    out.add(data + "\r\n");
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, RawResponse msg, List<Object> out) throws Exception {
    if (msg.getType() == ResponseType.TAGGED) {
      Response response = new BaseResponse().fromRawResponse(msg);
      out.add(response);
    } else if (msg.getType() == ResponseType.CONTINUATION) {
      Response response = new ContinuationResponse().fromRawResponse(msg);
      out.add(response);
    }
  }
}
