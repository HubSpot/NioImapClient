package com.hubspot.imap.imap;

import com.google.seventeen.common.base.Splitter;
import com.hubspot.imap.imap.response.RawResponse;
import com.hubspot.imap.imap.response.ResponseCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

public class ImapResponseDecoder extends ReplayingDecoder<Void> {
  private static final Splitter SPLITTER = Splitter.on(" ").limit(2).omitEmptyStrings().trimResults();
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapResponseDecoder.class);
  private static final Charset CHARSET = Charset.forName("UTF-8");

  private RawResponse response = new RawResponse();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    ByteBufInputStream inputStream = new ByteBufInputStream(in);

    while (inputStream.available() > 0) {
      String line = inputStream.readLine();
      LOGGER.debug("IMAP RCV: {}", line);
      List<String> parts = SPLITTER.splitToList(line);

      if (parts.get(0).equals("*") || parts.get(0).equals("+")) {
        response.addUntaggedLine(parts.get(1));
        checkpoint();
      } else {
        String tag = parts.get(0);
        List<String> taggedResponseParts = SPLITTER.splitToList(parts.get(1));
        ResponseCode code = ResponseCode.valueOf(taggedResponseParts.get(0));

        response.setTag(tag);
        response.setResponseCode(code);
        response.setResponseMessage(taggedResponseParts.get(1));

        out.add(response);
        response = new RawResponse();
        checkpoint();
      }
    }
  }
}
