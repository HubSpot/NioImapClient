package com.hubspot.imap.utils.parsers;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class NumberParser implements ByteBufProcessor {

  private final int maxLength;
  private final SoftReferencedAppendableCharSequence sequenceRef;
  private int size = 0;

  private AppendableCharSequence seq;

  public NumberParser(SoftReferencedAppendableCharSequence sequenceRef, int maxLength) {
    this.sequenceRef = sequenceRef;
    this.maxLength = maxLength;
  }

  public long parse(ByteBuf in) {
    seq = sequenceRef.get();

    seq.reset();
    size = 0;
    int i = in.forEachByte(this);
    in.readerIndex(i);

    Long result = Long.parseLong(seq.toString());
    seq = null;

    return result;
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
        throw new TooLongFrameException("Number is larger than " + maxLength + " bytes.");
      }

      seq.append(nextByte);
      size++;
      return true;
    }
  }
}
