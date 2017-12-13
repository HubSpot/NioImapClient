package com.hubspot.imap.utils.parsers.string;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.ByteBufParser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Signal;

public class BufferedBodyParser implements ByteBufParser<Optional<byte[]>>, Closeable {

  private static final Signal REPLAYING_SIGNAL;
  static {
    REPLAYING_SIGNAL = Signal.valueOf(ReplayingDecoder.class, "REPLAY");
  }

  private final AtomOrStringParser stringParser;
  private final LiteralStringSizeParser sizeParser;

  private State state;
  private int expectedSize;
  private int size;
  private int pos;
  private ByteBuf buf;

  public BufferedBodyParser(SoftReferencedAppendableCharSequence sequenceRef) {
    this.stringParser = new AtomOrStringParser(sequenceRef, 10000);
    this.sizeParser = new LiteralStringSizeParser(sequenceRef);

    this.expectedSize = -1;
    this.size = 0;
    this.state = State.START;
  }

  @Override
  public Optional<byte[]> parse(ByteBuf in) {
    for (;;) {
      switch (state) {
        case START:
          char c = ((char) in.readUnsignedByte());
          if (c == '{') {
            in.readerIndex(in.readerIndex() - 1);
            expectedSize = sizeParser.parse(in);
            state = State.SKIP_CRLF;
            return Optional.empty();
          } else if (Character.isWhitespace(c)) {
            continue;
          } else {
            in.readerIndex(in.readerIndex() - 1);
            state = State.PARSE_STRING;
            continue;
          }
        case SKIP_CRLF:
          in.readBytes(2);
          state = State.PARSE_SIZE;
          continue;
        case PARSE_STRING:
          return Optional.of(stringParser.parse(in).getBytes(StandardCharsets.UTF_8));
        case PARSE_SIZE:
          if (buf == null) {
            buf = PooledByteBufAllocator.DEFAULT.buffer(expectedSize);
          }

          pos = in.readerIndex();
          try {
            while (size < expectedSize) {
              buf.writeByte((char) in.readUnsignedByte());
              inc();
            }
          } catch (Signal e) {
            e.expect(REPLAYING_SIGNAL);
            in.readerIndex(pos);
            return Optional.empty();
          }

          byte[] result = new byte[buf.readableBytes()];
          buf.getBytes(buf.readerIndex(), result);
          reset();

          return Optional.of(result);
      }
    }

  }

  private void reset() {
    if (buf != null) {
      buf.release();
      buf = null;
    }

    size = 0;
    expectedSize = -1;
    pos = 0;
    state = State.START;
  }

  private void inc() {
    size++;
    pos++;
  }

  @Override
  public void close() throws IOException {
    if (buf != null) {
      buf.release();
      buf = null;
    }
  }

  private enum State {
    START,
    SKIP_CRLF,
    PARSE_SIZE,
    PARSE_STRING
    ;
  }
}
