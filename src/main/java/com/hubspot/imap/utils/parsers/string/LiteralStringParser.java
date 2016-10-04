package com.hubspot.imap.utils.parsers.string;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.ByteBufParser;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.AppendableCharSequence;

public class LiteralStringParser implements ByteBufParser<String> {
  private final SoftReferencedAppendableCharSequence sequenceRef;
  private final AtomOrStringParser stringParser;
  private final LiteralStringSizeParser sizeParser;

  private int expectedSize;
  private int size;

  public LiteralStringParser(SoftReferencedAppendableCharSequence sequenceRef) {
    this.sequenceRef = sequenceRef;
    this.stringParser = new AtomOrStringParser(sequenceRef, 10000);
    this.sizeParser = new LiteralStringSizeParser(sequenceRef);

    this.expectedSize = -1;
    this.size = 0;
  }

  @Override
  public String parse(ByteBuf in) {
    AppendableCharSequence seq = sequenceRef.get();

    seq.reset();
    size = 0;
    expectedSize = -1;
    for (;;) {
      char c = ((char) in.readUnsignedByte());
      if (c == '{' && expectedSize < 0) {
        in.readerIndex(in.readerIndex() - 1);
        expectedSize = sizeParser.parse(in);

        in.readerIndex(in.readerIndex() + 2); // Skip CRLF
      } else if (expectedSize >= 0) {
        seq.reset();
        seq.append(c);
        size++;

        while (size < expectedSize) {
          c = ((char) in.readUnsignedByte());
          seq.append(c);
          size++;
        }

        return seq.toString();
      } else if (Character.isWhitespace(c)) {
        continue;
      } else {
        in.readerIndex(in.readerIndex() - 1);
        return stringParser.parse(in);
      }
    }
  }

}
