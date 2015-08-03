package com.hubspot.imap.protocol;

import com.google.common.collect.Lists;
import com.google.seventeen.common.primitives.Ints;
import com.hubspot.imap.protocol.ResponseDecoder.State;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.exceptions.UnknownFetchItemTypeException;
import com.hubspot.imap.protocol.folder.FolderAttribute;
import com.hubspot.imap.protocol.folder.FolderFlags;
import com.hubspot.imap.protocol.folder.FolderMetadata;
import com.hubspot.imap.protocol.message.Envelope;
import com.hubspot.imap.protocol.message.ImapMessage;
import com.hubspot.imap.protocol.response.ContinuationResponse;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.events.ByeEvent;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedIntResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedIntResponse.Builder;
import com.hubspot.imap.protocol.response.untagged.UntaggedResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedResponseType;
import com.hubspot.imap.utils.parsers.ArrayParser;
import com.hubspot.imap.utils.parsers.AtomOrStringParser;
import com.hubspot.imap.utils.parsers.LineParser;
import com.hubspot.imap.utils.parsers.NestedArrayParser;
import com.hubspot.imap.utils.parsers.NumberParser;
import com.hubspot.imap.utils.parsers.WordParser;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.AppendableCharSequence;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class handles decoding of IMAP responses. It handles 3 main types of responses
 *
 *  - Tagged - Final command repsonse. Prefixed with a command tag (e.x. A01)
 *  - Continuation - For when the server needs more info from the client. Prefixed with a '+'
 *  - Untagged  - Intermediate response, provides extra data for tagged response. Prefixed with a '*'. (Note: a server can send an anonymous tagged response at any time, even outside the context of a tagged command)
 *
 * Tagged and continuation responses are fairly straightforward. Untagged responses are broken down in to 3 sub types:
 *
 *   - Standard - Response type followed by response data
 *   - OK - Starts with 'OK' followed by bracketed response data
 *   - Value - Starts with an int value, followed by response type
 *
 * Unless the current command specifically requests notification of untagged responses (i.e. IDLE), untagged responses are collected and added to the body of the tagged response once the tag is received.
 */
public class ResponseDecoder extends ReplayingDecoder<State> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseDecoder.class);

  private static final char UNTAGGED_PREFIX = '*';
  private static final char CONTINUATION_PREFIX = '+';
  private static final char TAGGED_PREFIX = 'A'; // This isn't necessarily true from the IMAP spec, but this client always prefixes tags with 'A'

  private static final char LPAREN = '(';
  private static final char RPAREN = ')';

  // This AppendableCharSequence is shared by all of the parsers for memory efficiency.
  private final AppendableCharSequence charSeq;
  private final LineParser lineParser;
  private final WordParser wordParser;
  private final AtomOrStringParser atomOrStringParser;
  private final ArrayParser arrayParser;
  private final NumberParser numberParser;
  private final EnvelopeParser envelopeParser;
  private final NestedArrayParser.Recycler<String> nestedArrayParserRecycler;

  private List<Object> untaggedResponses;
  private TaggedResponse.Builder responseBuilder;

  private ImapMessage.Builder currentMessage;

  public ResponseDecoder() {
    super(State.SKIP_CONTROL_CHARS);
    this.charSeq = new AppendableCharSequence(100000);
    this.lineParser = new LineParser(charSeq, 100000);
    this.wordParser = new WordParser(charSeq, 100000);
    this.atomOrStringParser = new AtomOrStringParser(charSeq, 100000);
    this.numberParser = new NumberParser(charSeq, 19);
    this.arrayParser = new ArrayParser(charSeq);
    this.envelopeParser = new EnvelopeParser();
    this.nestedArrayParserRecycler = new NestedArrayParser.Recycler<>(atomOrStringParser);

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
    FETCH,
    RESET;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    for (;;) {
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
            handleUntaggedValue(type, word, ctx);
          } else {
            if (word.equalsIgnoreCase(ResponseCode.OK.name())) {
              checkpoint(State.UNTAGGED_OK);
            } else {
              UntaggedResponseType type = UntaggedResponseType.getResponseType(word);
              handleUntagged(type, in, ctx);
            }
          }
          break;
        case UNTAGGED_OK:
          handleUntagged(in, ctx);
          break;
        case CONTINUATION:
          handleContinuation(in, out);
          break;
        case TAGGED:
          handleTagged(in, out);
          break;
        case FETCH:
          parseFetch(ctx, in, out);
          break;
        case RESET:
          reset(in);
          break;
      }
    }
  }

  private void parseFetch(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws UnknownFetchItemTypeException {
    skipControlCharacters(in);

    char next = ((char) in.readUnsignedByte());
    if (next != LPAREN && next != RPAREN) {
      in.readerIndex(in.readerIndex() - 1);
    } else if (next == RPAREN) { // Check to see if this is the end of this fetch response
      char second = ((char) in.readUnsignedByte());
      if (second == HttpConstants.CR || second == HttpConstants.LF) {
        // At the end of the fetch, add the current message to the untagged responses and reset
        untaggedResponses.add(currentMessage.build());
        currentMessage = null;
        checkpoint(State.RESET);
        return;
      } else {
        in.readerIndex(in.readerIndex() - 2);
      }
    }

    String fetchItemString = wordParser.parse(in).toString();
    if (StringUtils.isBlank(fetchItemString)) {
      checkpoint(State.FETCH);
      return;
    }

    FetchDataItemType fetchType = FetchDataItemType.getFetchType(fetchItemString);
    switch (fetchType) {
      case FLAGS:
        List<String> flags = arrayParser.parse(in);
        currentMessage.setFlagStrings(flags);
        break;
      case INTERNALDATE:
        String internalDate = atomOrStringParser.parse(in);
        currentMessage.setInternalDate(internalDate);
        break;
      case RFC822_SIZE:
        currentMessage.setSize(Ints.checkedCast(numberParser.parse(in)));
        break;
      case UID:
        currentMessage.setUid(numberParser.parse(in));
        break;
      case ENVELOPE:
        currentMessage.setEnvelope(parseEnvelope(in));
        break;
      case X_GM_MSGID:
        currentMessage.setGmailMessageId(numberParser.parse(in));
        break;
      case X_GM_THRID:
        currentMessage.setGmailThreadId(numberParser.parse(in));
        break;
      case INVALID:
      default:
        // This is really bad because we need to know what type of response to parse for each tag.
        // Given an unknown fetch type, we can't find the next fetch type tag, so we just have to stop.
        lineParser.parse(in);
        checkpoint(State.RESET);
        throw new UnknownFetchItemTypeException(fetchItemString);
    }

    checkpoint(State.FETCH);
  }

  private void handleTagged(ByteBuf in, List<Object> out) {
    String tag = wordParser.parse(in).toString();
    String codeString = wordParser.parse(in).toString();
    ResponseCode code = ResponseCode.valueOf(codeString);
    String message = lineParser.parse(in).toString();

    responseBuilder.setTag(tag);
    responseBuilder.setCode(code);
    responseBuilder.setMessage(message);
    responseBuilder.setUntagged(Lists.newArrayList(untaggedResponses));

    untaggedResponses.clear();

    write(out);
  }

  private void handleUntaggedValue(UntaggedResponseType type, String value, ChannelHandlerContext ctx) {
    switch (type) {
      case FETCH:
        currentMessage = new ImapMessage.Builder()
            .setMessageNumber(Long.parseLong(value));
        checkpoint(State.FETCH);
        return;
      case RECENT:
        untaggedResponses.add(handleIntResponse(type, value));
        break;
      case EXPUNGE:
      case EXISTS:
        handleMessageCountResponse(type, value, ctx);
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

  private void handleUntagged(ByteBuf in, ChannelHandlerContext ctx) {
    String responseTypeString = wordParser.parse(in).toString();
    UntaggedResponseType type = UntaggedResponseType.getResponseType(responseTypeString);
    handleUntagged(type, in, ctx);
  }

  private void handleUntagged(UntaggedResponseType type, ByteBuf in, ChannelHandlerContext ctx) {
    switch (type) {
      case BYE:
        handleBye(in, ctx);
        break;
      case LIST:
        untaggedResponses.add(parseFolderMetadata(in));
        break;
      case PERMANENTFLAGS:
        untaggedResponses.add(parseFlags(in, true));
        break;
      case FLAGS:
        untaggedResponses.add(parseFlags(in, false));
        break;
      // Bracketed responses. Fallthrough here is intentional.
      case HIGHESTMODSEQ:
      case UIDNEXT:
      case UIDVALIDITY:
        untaggedResponses.add(parseIntResponse(type, in));
        break;
      default:
        untaggedResponses.add(lineParser.parse(in).toString());
    }

    checkpoint(State.RESET);
  }

  private void handleMessageCountResponse(UntaggedResponseType type, String value, ChannelHandlerContext ctx) {
    UntaggedIntResponse intResponse = handleIntResponse(type, value);
    untaggedResponses.add(intResponse);

  }

  private void handleBye(ByteBuf in, ChannelHandlerContext handlerContext) {
    String message = lineParser.parse(in).toString();

    UntaggedResponse response = new UntaggedResponse.Builder()
        .setType(UntaggedResponseType.BYE)
        .setMessage(message)
        .build();

    untaggedResponses.add(response);

    handlerContext.fireUserEventTriggered(new ByeEvent(response));
  }

  private UntaggedIntResponse handleIntResponse(UntaggedResponseType type, String value) {
    return new UntaggedIntResponse.Builder()
        .setType(type)
        .setValue(Long.parseLong(value))
        .build();
  }

  private Envelope parseEnvelope(ByteBuf in) {
    NestedArrayParser<String> arrayParser = nestedArrayParserRecycler.get();
    List<Object> envelopeData = arrayParser.parse(in);
    arrayParser.recycle();

    return envelopeParser.parse(envelopeData);
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

    String context = atomOrStringParser.parse(in);
    String name = atomOrStringParser.parse(in);

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

  private UntaggedIntResponse parseIntResponse(UntaggedResponseType type, ByteBuf in) {
    long value = numberParser.parse(in);
    wordParser.parse(in); // Clear any extra non-digit data

    return new Builder()
        .setType(type)
        .setValue(value)
        .build();
  }

  private void write(List<Object> out) {
    out.add(responseBuilder.build());

    checkpoint(State.RESET);
  }

  /**
   * Reset checks to see if we are at the end of this response line. If not it fast forwards the buffer to the end of this line to prepare for the next response.
   * @param in
   */
  private void reset(ByteBuf in) {
    char c = (char) in.readUnsignedByte();
    if (c == UNTAGGED_PREFIX || c == CONTINUATION_PREFIX || c == TAGGED_PREFIX) { // We are already at the end of the line
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
