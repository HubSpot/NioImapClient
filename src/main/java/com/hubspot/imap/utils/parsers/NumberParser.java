package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.TooLongFrameException;

public class NumberParser implements ByteBufProcessor {
  private final int maxLength;
  private long out = 0;
  private int size = 0;

  public NumberParser(int maxLength) {
    this.maxLength = maxLength;
  }

  public long parse(ByteBuf in) {
    out = 0;
    size = 0;
    int i = in.forEachByte(this);
    in.readerIndex(i);
    return out;
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

      out += Character.getNumericValue(nextByte) * Math.pow(10, size);
      size++;
      return true;
    }
  }
}
