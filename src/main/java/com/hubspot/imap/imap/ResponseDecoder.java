package com.hubspot.imap.imap;

import com.hubspot.imap.ImapClient;
import com.hubspot.imap.imap.ResponseDecoder.State;
import com.hubspot.imap.imap.folder.FolderFlags;
import com.hubspot.imap.imap.folder.FolderAttribute;
import com.hubspot.imap.imap.folder.FolderMetadata;
import com.hubspot.imap.imap.response.ContinuationResponse;
import com.hubspot.imap.imap.response.ResponseCode;
import com.hubspot.imap.imap.response.tagged.TaggedResponse;
import com.hubspot.imap.imap.response.untagged.UntaggedIntResponse;
import com.hubspot.imap.imap.response.untagged.UntaggedIntResponse.Builder;
import com.hubspot.imap.imap.response.untagged.UntaggedResponseType;
import com.hubspot.imap.utils.parsers.ArrayParser;
import com.hubspot.imap.utils.parsers.LineParser;
import com.hubspot.imap.utils.parsers.OptionallyQuotedStringParser;
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
import java.util.Set;
import java.util.stream.Collectors;

public class ResponseDecoder extends ReplayingDecoder<State> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseDecoder.class);

  private static final char UNTAGGED_PREFIX = '*';
  private static final char CONTINUATION_PREFIX = '+';
  private static final char TAGGED_PREFIX = 'A'; // This isn't necessarily true from the IMAP spec, but this client always prefixes tags with 'A'

  private final AppendableCharSequence charSeq;
  private final LineParser lineParser;
  private final WordParser wordParser;
  private final OptionallyQuotedStringParser quotedStringParser;
  private final ArrayParser arrayParser;

  private ImapClient client;
  private List<Object> untaggedResponses;
  private TaggedResponse.Builder responseBuilder;

  public ResponseDecoder(ImapClient client) {
    super(State.SKIP_CONTROL_CHARS);
    this.charSeq = new AppendableCharSequence(8192);
    this.lineParser = new LineParser(charSeq, 8192);
    this.wordParser = new WordParser(charSeq, 8192);
    this.quotedStringParser = new OptionallyQuotedStringParser(charSeq, 8192);
    this.arrayParser = new ArrayParser(charSeq);

    this.client = client;
    this.untaggedResponses = new ArrayList<>();
    this.responseBuilder = new TaggedResponse.Builder();
  }

  enum State {
    SKIP_CONTROL_CHARS,
    START_RESPONSE,
    UNTAGGED,
    UNTAGGED_OK,
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
          handleUntaggedValue(type, word);
        } else {
          if (word.equalsIgnoreCase(ResponseCode.OK.name())) {
            checkpoint(State.UNTAGGED_OK);
          } else {
            UntaggedResponseType type = UntaggedResponseType.getResponseType(word);
            handleUntagged(type, in, out);
          }
        }
        break;
      case UNTAGGED_OK:
        handleUntagged(in, out);
        break;
      case CONTINUATION:
        handleContinuation(in, out);
        break;
      case TAGGED:
        handleTagged(in, out);
        break;
      case RESET:
        reset(in);
        break;
    }
  }

  private void handleTagged(ByteBuf in, List<Object> out) {
    String tag = wordParser.parse(in).toString();
    String codeString = wordParser.parse(in).toString();
    ResponseCode code = ResponseCode.valueOf(codeString);
    String message = lineParser.parse(in).toString();

    responseBuilder.setTag(tag);
    responseBuilder.setCode(code);
    responseBuilder.setMessage(message);
    responseBuilder.setUntagged(untaggedResponses);

    write(in, out);
  }

  private void handleUntaggedValue(UntaggedResponseType type, String value) {
    switch (type) {
      case RECENT:
      case EXISTS:
        untaggedResponses.add(handleIntResponse(type, value));
        break;
      default:
        untaggedResponses.add(value);
    }

    checkpoint(State.RESET);
  }

  private void handleContinuation(ByteBuf in, List<Object> out) {
    String message = lineParser.parse(in).toString();

    ContinuationResponse response = new ContinuationResponse.Builder().setMessage(message).build();
    out.add(response);

    checkpoint(State.RESET);
  }

  private void handleUntagged(ByteBuf in, List<Object> out) {
    String responseTypeString = wordParser.parse(in).toString();
    UntaggedResponseType type = UntaggedResponseType.getResponseType(responseTypeString);
    handleUntagged(type, in, out);
  }

  private void handleUntagged(UntaggedResponseType type, ByteBuf in, List<Object> out) {
    switch (type) {
      case LIST:
        untaggedResponses.add(parseFolderMetadata(in));
        break;
      case PERMANENTFLAGS:
        untaggedResponses.add(parseFlags(in, true));
        break;
      case FLAGS:
        untaggedResponses.add(parseFlags(in, false));
        break;
      case HIGHESTMODSEQ:
      case UIDNEXT:
      case UIDVALIDITY:
        untaggedResponses.add(parseBracketedResponse(type, in));
        break;
      default:
        untaggedResponses.add(lineParser.parse(in).toString());
    }

    checkpoint(State.RESET);
  }

  private UntaggedIntResponse handleIntResponse(UntaggedResponseType type, String value) {
    return new UntaggedIntResponse.Builder()
        .setType(type)
        .setValue(Long.parseLong(value))
        .build();
  }

  private FolderFlags parseFlags(ByteBuf in, boolean permanent) {
    skipControlCharacters(in);
    List<String> flags = arrayParser.parse(in);

    lineParser.parse(in);

    return FolderFlags.fromStrings(flags, permanent);
  }

  private FolderMetadata parseFolderMetadata(ByteBuf in) {
    skipControlCharacters(in);
    Set<FolderAttribute> attributes = parseFolderAttributes(in);

    String context = quotedStringParser.parse(in).toString();
    String name = quotedStringParser.parse(in).toString();

    // Make sure we parse to the end of the line
    lineParser.parse(in);

    return new FolderMetadata.Builder()
        .addAllAttributes(attributes)
        .setContext(context)
        .setName(name)
        .build();
  }

  private Set<FolderAttribute> parseFolderAttributes(ByteBuf in) {
    return arrayParser.parse(in).stream()
        .map(FolderAttribute::getAttribute)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
  }

  private UntaggedIntResponse parseBracketedResponse(UntaggedResponseType type, ByteBuf in) {
    String value = wordParser.parse(in).toString();
    if (value.endsWith("]")) {
      value = value.substring(0, value.length() - 1);
    }

    return new Builder()
        .setType(type)
        .setValue(Long.parseLong(value))
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

    checkpoint(State.START_RESPONSE);
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
