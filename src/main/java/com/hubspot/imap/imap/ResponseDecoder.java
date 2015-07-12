package com.hubspot.imap.imap;

import com.hubspot.imap.imap.ResponseDecoder.State;
import com.hubspot.imap.imap.response.Response;
import com.hubspot.imap.imap.response.Response.ResponseType;
import com.hubspot.imap.imap.response.ResponseCode;
import com.hubspot.imap.utils.parsers.LineParser;
import com.hubspot.imap.utils.parsers.UntaggedResponseType;
import com.hubspot.imap.utils.parsers.WordParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.internal.AppendableCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ResponseDecoder extends ReplayingDecoder<State> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseDecoder.class);

  private static final char UNTAGGED_PREFIX = '*';
  private static final char CONTINUATION_PREFIX = '+';
  private static final char TAGGED_PREFIX = 'A'; // This isn't necessarily true from the IMAP spec, but this client always prefixes tags with 'A'

  private final AppendableCharSequence charSeq;
  private final LineParser lineParser;
  private final WordParser wordParser;
  private boolean expectsTag;

  private Response.Builder responseBuilder;

  public ResponseDecoder() {
    super(State.SKIP_CONTROL_CHARS);
    this.charSeq = new AppendableCharSequence(8093);
    this.lineParser = new LineParser(charSeq, 8093);
    this.wordParser = new WordParser(charSeq, 8093);
    this.responseBuilder = new Response.Builder();
  }

  enum State {
    SKIP_CONTROL_CHARS,
    START_RESPONSE,
    UNTAGGED,
    CONTINUATION,
    TAGGED;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    switch (state()) {
      case SKIP_CONTROL_CHARS:
        try {
          skipControlCharacters(in);
          checkpoint(State.START_RESPONSE);
        } finally {
          checkpoint();
        }
      case START_RESPONSE:
        char c = ((char) in.readUnsignedByte());
        if (c == UNTAGGED_PREFIX) {
          checkpoint(State.UNTAGGED);
        } else if (c == CONTINUATION_PREFIX) {
          checkpoint(State.CONTINUATION);
        } else {
          in.readerIndex(in.readerIndex() - 1); // We want the whole tag (A1) not just the int
          checkpoint(State.TAGGED);
        }
        break;
      case UNTAGGED:
        skipControlCharacters(in);
        String word = wordParser.parse(in).toString();
        UntaggedResponseType type = UntaggedResponseType.getResponseType(word);
        String line = lineParser.parse(in).toString();
        handleUntagged(line, out);
        break;
      case TAGGED:
        String tag = wordParser.parse(in).toString();
        skipControlCharacters(in);
        ResponseCode code = ResponseCode.valueOf(wordParser.parse(in).toString());
        String message = lineParser.parse(in).toString();
        handleTagged(tag, code, message, out);
        break;
    }
  }

  private void handleTagged(String tag, ResponseCode code, String message, List<Object> out) {
    responseBuilder.setType(ResponseType.TAGGED);
    responseBuilder.setTag(tag);
    responseBuilder.setCode(code);
    responseBuilder.setMessage(message);

    write(out);
  }

  private void handleUntagged(String untagged, List<Object> out) {
    LOGGER.info("Got untagged response: {}", untagged);
    checkpoint(State.SKIP_CONTROL_CHARS);
  }

  private void write(List<Object> out) {
    out.add(responseBuilder.build());
    checkpoint(State.SKIP_CONTROL_CHARS);
  }

  private String getWord(ByteBuf buffer) {
    charSeq.reset();
    for (;;) {
      char c = (char) buffer.readUnsignedByte();
      if (Character.isWhitespace(c)) {
        break;
      }

      charSeq.append(c);
    }

    return charSeq.toString();
  }

  private static void skipControlCharacters(ByteBuf buffer) {
    for (;;) {
      char c = (char) buffer.readUnsignedByte();
      if (!Character.isISOControl(c) &&
          !Character.isWhitespace(c)) {
        buffer.readerIndex(buffer.readerIndex() - 1);
        break;
      }
    }
  }
}
