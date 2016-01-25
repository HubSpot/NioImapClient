package com.hubspot.imap.protocol;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.client.ImapClientState;
import com.hubspot.imap.protocol.ResponseDecoder.State;
import com.hubspot.imap.protocol.command.fetch.StreamingFetchCommand;
import com.hubspot.imap.protocol.command.fetch.UidCommand;
import com.hubspot.imap.protocol.command.fetch.items.FetchDataItem.FetchDataItemType;
import com.hubspot.imap.protocol.exceptions.ResponseParseException;
import com.hubspot.imap.protocol.exceptions.UnknownFetchItemTypeException;
import com.hubspot.imap.protocol.extension.gmail.GMailLabel;
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
import com.hubspot.imap.protocol.response.untagged.UntaggedSearchResponse;
import com.hubspot.imap.utils.CommandUtils;
import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.AtomOrStringParser;
import com.hubspot.imap.utils.parsers.FetchResponseTypeParser;
import com.hubspot.imap.utils.parsers.LineParser;
import com.hubspot.imap.utils.parsers.LiteralStringParser;
import com.hubspot.imap.utils.parsers.NestedArrayParser;
import com.hubspot.imap.utils.parsers.NumberParser;
import com.hubspot.imap.utils.parsers.WordParser;
import com.hubspot.imap.utils.parsers.fetch.EnvelopeParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
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

  private static final MessageServiceFactory MESSAGE_SERVICE_FACTORY;

  static {
    try {
      MESSAGE_SERVICE_FACTORY = MessageServiceFactory.newInstance();
    } catch (MimeException e) {
      throw Throwables.propagate(e);
    }
  }

  private static final char UNTAGGED_PREFIX = '*';
  private static final char CONTINUATION_PREFIX = '+';
  private static final char TAGGED_PREFIX = 'A'; // This isn't necessarily true from the IMAP spec, but this client always prefixes tags with 'A'

  private static final char LPAREN = '(';
  private static final char RPAREN = ')';

  private final ImapClientState clientState;
  private final EventExecutorGroup executorGroup;

  // This AppendableCharSequence is shared by all of the parsers for memory efficiency.
  private final SoftReferencedAppendableCharSequence charSeq;
  private final LineParser lineParser;
  private final WordParser wordParser;
  private final FetchResponseTypeParser fetchResponseTypeParser;
  private final AtomOrStringParser atomOrStringParser;
  private final LiteralStringParser literalStringParser;
  private final NumberParser numberParser;
  private final EnvelopeParser envelopeParser;
  private final NestedArrayParser.Recycler<String> nestedArrayParserRecycler;
  private final DefaultMessageBuilder messageBuilder;

  private List<Object> untaggedResponses;
  private TaggedResponse.Builder responseBuilder;

  private ImapMessage.Builder currentMessage;

  public ResponseDecoder(ImapConfiguration configuration, ImapClientState clientState, EventExecutorGroup executorGroup) {
    super(State.SKIP_CONTROL_CHARS);
    this.clientState = clientState;
    this.executorGroup = executorGroup;

    this.charSeq = new SoftReferencedAppendableCharSequence(configuration);
    this.lineParser = new LineParser(charSeq, configuration.getMaxLineLength());
    this.wordParser = new WordParser(charSeq, configuration.getMaxLineLength());
    this.fetchResponseTypeParser = new FetchResponseTypeParser(charSeq, configuration.getMaxLineLength());
    this.atomOrStringParser = new AtomOrStringParser(charSeq, configuration.getMaxLineLength());
    this.literalStringParser = new LiteralStringParser(charSeq);
    this.numberParser = new NumberParser(charSeq, 19);
    this.envelopeParser = new EnvelopeParser();
    this.nestedArrayParserRecycler = new NestedArrayParser.Recycler<>(literalStringParser);
    this.messageBuilder = ((DefaultMessageBuilder) MESSAGE_SERVICE_FACTORY.newMessageBuilder());

    MimeConfig mimeConfig = new MimeConfig();
    mimeConfig.setMaxLineLen(configuration.getMaxLineLength());
    mimeConfig.setMaxHeaderLen(configuration.getMaxLineLength());
    messageBuilder.setMimeEntityConfig(mimeConfig);

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
      if (LOGGER.isDebugEnabled()) {
        dumpLine("RCV", in);
      }

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
          String word = wordParser.parse(in);
          if (NumberUtils.isDigits(word)) {
            UntaggedResponseType type = UntaggedResponseType.getResponseType(wordParser.parse(in));
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
          try {
            parseFetch(in);
          } catch (Exception e) {
            if (state() != State.RESET) {
              lineParser.parse(in);
              checkpoint(State.RESET);
              throw e;
            }
          }
          break;
        case RESET:
          reset(in);
          break;
      }
    }
  }

  private void parseFetch(ByteBuf in) throws UnknownFetchItemTypeException, IOException, ResponseParseException {
    skipControlCharacters(in);

    char next = ((char) in.readUnsignedByte());
    if (next != LPAREN && next != RPAREN) {
      in.readerIndex(in.readerIndex() - 1);
    } else if (next == RPAREN) { // Check to see if this is the end of this fetch response
      char second = ((char) in.readUnsignedByte());
      if (second == HttpConstants.CR || second == HttpConstants.LF) {
        // At the end of the fetch, add the current message to the untagged responses and reset
        messageComplete();
        return;
      } else {
        in.readerIndex(in.readerIndex() - 2);
      }
    }

    String fetchItemString = fetchResponseTypeParser.parse(in);
    if (StringUtils.isBlank(fetchItemString)) {
      checkpoint(State.FETCH);
      return;
    }

    FetchDataItemType fetchType = FetchDataItemType.getFetchType(fetchItemString);
    switch (fetchType) {
      case FLAGS:
        List<String> flags = nestedArrayParserRecycler.get().parse(in).stream()
            .map(o -> ((String) o))
            .collect(Collectors.toList());
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
      case BODY:
        currentMessage.setBody(parseBody(in));
        break;
      case X_GM_MSGID:
        currentMessage.setGmailMessageId(numberParser.parse(in));
        break;
      case X_GM_THRID:
        currentMessage.setGmailThreadId(numberParser.parse(in));
        break;
      case X_GM_LABELS:
        currentMessage.setGMailLabels(
            nestedArrayParserRecycler.get().parse(in).stream()
                .map(o -> ((String) o))
                .map(GMailLabel::get)
                .collect(Collectors.toSet())
        );
        break;
      case INVALID:
      default:
        throw new UnknownFetchItemTypeException(fetchItemString);
    }

    checkpoint(State.FETCH);
  }

  private void messageComplete() {
    ImapMessage message = currentMessage.build();
    currentMessage = null;

    if (CommandUtils.isStreamingFetch(clientState.getCurrentCommand())) {
      StreamingFetchCommand fetchCommand;
      if (clientState.getCurrentCommand() instanceof UidCommand) {
        fetchCommand = ((StreamingFetchCommand) ((UidCommand) clientState.getCurrentCommand()).getWrappedCommand());
      } else {
        fetchCommand = ((StreamingFetchCommand) clientState.getCurrentCommand());
      }

      Future<Void> future = executorGroup.submit(() -> {
        fetchCommand.handle(message);
        return null;
      });

      untaggedResponses.add(future);
    } else {
      untaggedResponses.add(message);
    }

    checkpoint(State.RESET);
  }

  private void handleTagged(ByteBuf in, List<Object> out) {
    String tag = wordParser.parse(in);
    String codeString = wordParser.parse(in);
    ResponseCode code = ResponseCode.valueOf(codeString);
    String message = lineParser.parse(in);

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
    String message = lineParser.parse(in);

    ContinuationResponse response = new ContinuationResponse.Builder().setMessage(message).build();
    out.add(response);

    checkpoint(State.RESET);
  }

  private void handleUntagged(ByteBuf in, ChannelHandlerContext ctx) {
    String responseTypeString = wordParser.parse(in);
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
      case SEARCH:
        untaggedResponses.add(parseSearch(in));
        break;
      case HIGHESTMODSEQ:
      case UIDNEXT:
      case UIDVALIDITY:
        untaggedResponses.add(parseIntResponse(type, in));
        break;
      default:
        untaggedResponses.add(lineParser.parse(in));
    }

    checkpoint(State.RESET);
  }

  private void handleMessageCountResponse(UntaggedResponseType type, String value, ChannelHandlerContext ctx) {
    UntaggedIntResponse intResponse = handleIntResponse(type, value);
    untaggedResponses.add(intResponse);
  }

  private void handleBye(ByteBuf in, ChannelHandlerContext handlerContext) {
    String message = lineParser.parse(in);

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

  private Message parseBody(ByteBuf in) throws UnknownFetchItemTypeException, IOException, ResponseParseException {
    char c = ((char) in.readUnsignedByte());

    //String bodySection = ""; At some point we will need to actually store the body section that is being parsed below
    if (c != '[') {
      // This is effectively BODYSTRUCTURE which is not yet supported
      lineParser.parse(in);
      checkpoint(State.RESET);
      throw new UnknownFetchItemTypeException("BODYSTRUCTURE");
    } else {
      c = ((char) in.readUnsignedByte());
      while (c != ']') { // Skip characters within "[]"
        c = ((char) in.readUnsignedByte());
      }
    }
    String body = literalStringParser.parse(in);

    try (InputStream inputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))) {
      return messageBuilder.parseMessage(inputStream);
    } catch (IOException e) {
      throw new ResponseParseException(e);
    }
  }

  private Envelope parseEnvelope(ByteBuf in) {
    NestedArrayParser<String> arrayParser = nestedArrayParserRecycler.get();
    List<Object> envelopeData = arrayParser.parse(in);
    arrayParser.recycle();

    return envelopeParser.parse(envelopeData);
  }

  private FolderFlags parseFlags(ByteBuf in, boolean permanent) {
    skipControlCharacters(in);
    List<String> flags = nestedArrayParserRecycler.get().parse(in).stream()
        .map(o -> ((String) o))
        .collect(Collectors.toList());

    lineParser.parse(in);

    return FolderFlags.fromStrings(flags, permanent);
  }

  private UntaggedSearchResponse parseSearch(ByteBuf in) {
    List<Long> ids = new ArrayList<>();
    for (;;) {
      char c = ((char) in.readUnsignedByte());
      in.readerIndex(in.readerIndex() - 1);
      if (c == HttpConstants.CR || c == HttpConstants.LF) {
        lineParser.parse(in);
        break;
      }

      ids.add(Long.parseLong(atomOrStringParser.parse(in)));
    }

    return new UntaggedSearchResponse(ids);
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
    return nestedArrayParserRecycler.get().parse(in).stream()
        .map(o -> ((String) o))
        .map(FolderAttribute::getAttribute)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
  }

  private UntaggedIntResponse parseIntResponse(UntaggedResponseType type, ByteBuf in) {
    long value = numberParser.parse(in);
    lineParser.parse(in);

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
    String line = lineParser.parse(in);
    LOGGER.debug("{}: {}", prefix, line);

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
