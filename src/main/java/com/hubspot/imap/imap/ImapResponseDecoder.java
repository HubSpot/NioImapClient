package com.hubspot.imap.imap;

import com.google.seventeen.common.base.Splitter;
import com.hubspot.imap.imap.response.RawResponse;
import com.hubspot.imap.imap.response.Response.ResponseType;
import com.hubspot.imap.imap.response.ResponseCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ImapResponseDecoder extends ReplayingDecoder<Void> {
  private static final Splitter SPLITTER = Splitter.on(" ").limit(2).omitEmptyStrings().trimResults();
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapResponseDecoder.class);

  private RawResponse response = new RawResponse();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    ByteBufInputStream inputStream = new ByteBufInputStream(in);

    while (inputStream.available() > 0) {
      String line = inputStream.readLine();
      LOGGER.debug("IMAP RCV: {}", line);
      List<String> parts = SPLITTER.splitToList(line);

      if (parts.get(0).equals("*")) {
        response.addUntaggedLine(parts.get(1));
        checkpoint();
      } else if (parts.get(0).equals("+")) {
        handleContinuation(parts.get(1), out);
      } else {
        handleTagged(parts.get(0), parts.get(1), out);
      }
    }
  }

  private void handleContinuation(String message, List<Object> out) {
    response.setType(ResponseType.CONTINUATION);
    response.setResponseMessage(message);
    response.setResponseCode(ResponseCode.NONE);

    out.add(response);
    response = new RawResponse();
    checkpoint();
  }

  private void handleTagged(String tag, String message, List<Object> out) {
    List<String> taggedResponseParts = SPLITTER.splitToList(message);
    ResponseCode code = ResponseCode.valueOf(taggedResponseParts.get(0));

    response.setType(ResponseType.TAGGED);
    response.setTag(tag);
    response.setResponseCode(code);
    response.setResponseMessage(taggedResponseParts.get(1));

    out.add(response);
    response = new RawResponse();
    checkpoint();
  }
}
