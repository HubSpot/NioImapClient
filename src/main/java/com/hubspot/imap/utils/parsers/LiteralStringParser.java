package com.hubspot.imap.utils.parsers;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.AppendableCharSequence;

public class LiteralStringParser implements ByteBufParser<String> {

  private final SoftReferencedAppendableCharSequence sequenceRef;
  private final AtomOrStringParser stringParser;
  private final SizeParser sizeParser;

  public LiteralStringParser(SoftReferencedAppendableCharSequence sequenceRef) {
    this.sequenceRef = sequenceRef;
    this.stringParser = new AtomOrStringParser(sequenceRef, 10000);
    this.sizeParser = new SizeParser(sequenceRef);
  }

  @Override
  public String parse(ByteBuf in) {
    AppendableCharSequence seq = sequenceRef.get();

    seq.reset();
    int size = 0;

    int expectedSize = -1;
    for (;;) {
      char c = ((char) in.readUnsignedByte());
      if (c == '{') {
        in.readerIndex(in.readerIndex() - 1);
        expectedSize = sizeParser.parse(in);
      } else if (c == '"'){
        in.readerIndex(in.readerIndex() - 1);
        return stringParser.parse(in);
      } else if (expectedSize >= 0) {
        if (c == HttpConstants.LF || c == HttpConstants.CR) {
          continue;
        }

        seq.reset();
        seq.append(c);
        size++;

        while (size < expectedSize) {
          char ch = ((char) in.readUnsignedByte());
          seq.append(ch);
          size++;
        }

        return seq.toString();
      }
    }
  }

  private class SizeParser implements ByteBufParser<Integer> {

    SoftReferencedAppendableCharSequence sequenceRef;

    public SizeParser(SoftReferencedAppendableCharSequence sequenceRef) {
      this.sequenceRef = sequenceRef;
    }

    @Override
    public Integer parse(ByteBuf in) {
      AppendableCharSequence seq = sequenceRef.get();

      seq.reset();
      boolean foundStart = false;

      for (;;) {
        char c = ((char) in.readUnsignedByte());

        if (c == '{') {
          foundStart = true;
        } else if (c == '}') {
          return Integer.parseInt(seq.toString());
        } else if (foundStart) {
          seq.append(c);
        }
      }
    }
  }
}
