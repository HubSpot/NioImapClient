package com.hubspot.imap.imap;

import com.google.seventeen.common.base.Splitter;
import com.hubspot.imap.imap.response.TaggedResponse;
import com.hubspot.imap.imap.response.TaggedResponse.ResponseType;
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

  private TaggedResponse.Builder responseBuilder = new TaggedResponse.Builder();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    ByteBufInputStream inputStream = new ByteBufInputStream(in);

    while (inputStream.available() > 0) {
      String line = inputStream.readLine();
      LOGGER.debug("IMAP RCV: {}", line);
      List<String> parts = SPLITTER.splitToList(line);

      if (parts.get(0).equals("*")) {
        responseBuilder.addUntagged(parts.get(1));
        checkpoint();
      } else if (parts.get(0).equals("+")) {
        handleContinuation(parts.get(1), out);
      } else {
        handleTagged(parts.get(0), parts.get(1), out);
      }
    }
  }

  private void handleContinuation(String message, List<Object> out) {
    responseBuilder.setType(ResponseType.CONTINUATION);
    responseBuilder.setMessage(message);
    responseBuilder.setCode(ResponseCode.NONE);

    write(out);
  }

  private void handleTagged(String tag, String message, List<Object> out) {
    List<String> taggedResponseParts = SPLITTER.splitToList(message);
    ResponseCode code = ResponseCode.valueOf(taggedResponseParts.get(0));

    responseBuilder.setType(ResponseType.TAGGED);
    responseBuilder.setTag(tag);
    responseBuilder.setCode(code);
    responseBuilder.setMessage(taggedResponseParts.get(1));

    write(out);
  }

  private void write(List<Object> out) {
    out.add(responseBuilder.build());
    responseBuilder = new TaggedResponse.Builder();
    checkpoint();
  }
}
