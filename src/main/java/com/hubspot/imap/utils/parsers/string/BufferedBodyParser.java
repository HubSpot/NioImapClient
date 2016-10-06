package com.hubspot.imap.utils.parsers.string;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.ByteBufParser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Signal;

public class BufferedBodyParser implements ByteBufParser<Optional<String>> {

  static final String REPLAY = ReplayingDecoder.class.getName().concat(".REPLAY");

  private final AtomOrStringParser stringParser;
  private final LiteralStringSizeParser sizeParser;

  private int expectedSize;
  private int size;
  private int pos;
  private ByteBuf buf;

  public BufferedBodyParser(SoftReferencedAppendableCharSequence sequenceRef) {
    this.stringParser = new AtomOrStringParser(sequenceRef, 10000);
    this.sizeParser = new LiteralStringSizeParser(sequenceRef);

    this.expectedSize = -1;
    this.size = 0;
  }

  @Override
  public Optional<String> parse(ByteBuf in) {
    for (;;) {
      char c = ((char) in.readUnsignedByte());
      if (c == '{' && expectedSize < 0) {
        in.readerIndex(in.readerIndex() - 1);
        expectedSize = sizeParser.parse(in);

        in.readBytes(2); // Skip CRLF
      } else if (expectedSize >= 0) {
        if (buf == null) {
          buf = PooledByteBufAllocator.DEFAULT.buffer(expectedSize);
        }

        pos = in.readerIndex() - 1;

        buf.writeByte(c);
        inc();

        try {
          while (size < expectedSize) {
            buf.writeByte((char) in.readUnsignedByte());
            inc();
          }
        } catch (Signal e) {
          if (e.toString().equalsIgnoreCase(REPLAY)) {
            in.readerIndex(pos);
            return Optional.empty();
          }

          throw e;
        }

        String result = buf.toString(StandardCharsets.UTF_8);
        reset();

        return Optional.of(result);
      } else if (Character.isWhitespace(c)) {
        continue;
      } else {
        in.readerIndex(in.readerIndex() - 1);
        return Optional.of(stringParser.parse(in));
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
  }

  private void inc() {
    size++;
    pos++;
  }
}
