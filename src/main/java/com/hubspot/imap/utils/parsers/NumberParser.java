package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class NumberParser implements ByteBufProcessor {
  private final int maxLength;
  private AppendableCharSequence seq;
  private int size = 0;

  public NumberParser(AppendableCharSequence seq, int maxLength) {
    this.seq = seq;
    this.maxLength = maxLength;
  }

  public long parse(ByteBuf in) {
    seq.reset();
    size = 0;
    int i = in.forEachByte(this);
    in.readerIndex(i);
    return Long.parseLong(seq.toString());
  }

  @Override
  public boolean process(byte value) throws Exception {
    char nextByte = (char) value;
    if (Character.isWhitespace(nextByte)) {
      return size == 0;
    } else if (!Character.isDigit(nextByte)) {
      return false;
    } else {
      if (size >= maxLength) {
        throw new TooLongFrameException(
            "Number is larger than " + maxLength +
                " bytes.");
      }

      seq.append(nextByte);
      size++;
      return true;
    }
  }
}
