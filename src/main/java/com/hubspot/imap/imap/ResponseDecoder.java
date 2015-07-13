package com.hubspot.imap.imap;

import com.hubspot.imap.imap.ResponseDecoder.State;
import com.hubspot.imap.imap.folder.FolderAttribute;
import com.hubspot.imap.imap.folder.FolderMetadata;
import com.hubspot.imap.imap.response.TaggedResponse;
import com.hubspot.imap.imap.response.TaggedResponse.ResponseType;
import com.hubspot.imap.imap.response.ResponseCode;
import com.hubspot.imap.imap.response.untagged.UntaggedResponseLine;
import com.hubspot.imap.utils.parsers.ArrayParser;
import com.hubspot.imap.utils.parsers.LineParser;
import com.hubspot.imap.utils.parsers.UntaggedResponseType;
import com.hubspot.imap.utils.parsers.WordParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.AppendableCharSequence;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResponseDecoder extends ReplayingDecoder<State> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseDecoder.class);

  private static final char UNTAGGED_PREFIX = '*';
  private static final char CONTINUATION_PREFIX = '+';
  private static final char TAGGED_PREFIX = 'A'; // This isn't necessarily true from the IMAP spec, but this client always prefixes tags with 'A'

  private final AppendableCharSequence charSeq;
  private final LineParser lineParser;
  private final WordParser wordParser;
  private final ArrayParser arrayParser;
  private boolean expectsTag = false;

  private List<Object> untaggedResponses;
  private TaggedResponse.Builder responseBuilder;

  public ResponseDecoder() {
    super(State.SKIP_CONTROL_CHARS);
    this.charSeq = new AppendableCharSequence(8192);
    this.lineParser = new LineParser(charSeq, 8192);
    this.wordParser = new WordParser(charSeq, 8192);
    this.arrayParser = new ArrayParser(charSeq);

    this.untaggedResponses = new ArrayList<>();
    this.responseBuilder = new TaggedResponse.Builder();
  }

  enum State {
    SKIP_CONTROL_CHARS,
    START_RESPONSE,
    UNTAGGED,
    CONTINUATION,
    TAGGED,
    RESET;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    dumpLine("RCV", in);
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
        if (NumberUtils.isDigits(word)) {
          UntaggedResponseType type = UntaggedResponseType.getResponseType(wordParser.parse(in).toString());
          handleUntaggedValue(type, word, in);
        } else {
          UntaggedResponseType type = UntaggedResponseType.getResponseType(word);
          handleUntagged(type, in, out);
        }
        break;
      case TAGGED:
        String tag = wordParser.parse(in).toString();
        skipControlCharacters(in);
        handleTagged(tag, in, out);
        break;
      case RESET:
        reset(in);
        break;
    }
  }

  private void handleTagged(String tag, ByteBuf in, List<Object> out) {
    String codeString = wordParser.parse(in).toString();
    ResponseCode code = ResponseCode.valueOf(codeString);
    String message = lineParser.parse(in).toString();

    responseBuilder.setType(ResponseType.TAGGED);
    responseBuilder.setTag(tag);
    responseBuilder.setCode(code);
    responseBuilder.setMessage(message);
    responseBuilder.setUntagged(untaggedResponses);

    write(in, out);
  }

  private void handleUntaggedValue(UntaggedResponseType type, String value, ByteBuf in) {
    switch (type) {
      case EXISTS:
        untaggedResponses.add(
            new UntaggedResponseLine.Builder()
                .setResponseType(type)
                .setValue(value)
                .build()
        );

        break;
      default:
        untaggedResponses.add(value);
    }

    checkpoint(State.RESET);
  }

  private void handleUntagged(UntaggedResponseType type, ByteBuf in, List<Object> out) {
    switch (type) {
      case LIST:
        untaggedResponses.add(parseFolderMetadata(in));
        break;
      default:
        untaggedResponses.add(lineParser.parse(in).toString());
    }

    checkpoint(State.RESET);
  }

  private FolderMetadata parseFolderMetadata(ByteBuf in) {
    List<FolderAttribute> attributes = arrayParser.parse(in).stream()
        .map(FolderAttribute::getAttribute)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());

    String context = wordParser.parse(in).toString();
    String name = wordParser.parse(in).toString();

    // Make sure we parse to the end of the line
    lineParser.parse(in);

    return new FolderMetadata.Builder()
        .addAllAttributes(attributes)
        .setContext(context)
        .setName(name)
        .build();
  }

  private void write(ByteBuf in, List<Object> out) {
    out.add(responseBuilder.build());

    checkpoint(State.RESET);
  }

  private void reset(ByteBuf in) {
    char c = (char) in.readUnsignedByte();
    if (c == UNTAGGED_PREFIX || c == CONTINUATION_PREFIX || c == TAGGED_PREFIX) {
      in.readerIndex(in.readerIndex() - 1);
    } else if (!(c == HttpConstants.CR || c == HttpConstants.LF)) {
      lineParser.parse(in);
    }

    checkpoint(State.SKIP_CONTROL_CHARS);
  }

  private void dumpLine(String prefix, ByteBuf in) {
    int index = in.readerIndex();
    String line = lineParser.parse(in).toString();
    LOGGER.info("{}: {}", prefix, line);

    in.readerIndex(index);
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
