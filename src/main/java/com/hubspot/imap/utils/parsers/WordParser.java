package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class WordParser implements ByteBufProcessor {
  private final AppendableCharSequence seq;
  private final int maxWordLength;
  private int size;

  public WordParser(AppendableCharSequence seq, int maxWordLength) {
    this.seq = seq;
    this.maxWordLength = maxWordLength;
  }

  public AppendableCharSequence parse(ByteBuf buffer) {
    seq.reset();
    size = 0;
    int i = buffer.forEachByte(this);
    buffer.readerIndex(i + 1);
    return seq;
  }

  @Override
  public boolean process(byte value) throws Exception {
    char nextByte = (char) value;
    if (Character.isWhitespace(nextByte)) {
      return false;
    } else {
      if (size >= maxWordLength) {
        throw new TooLongFrameException(
            "Word is larger than " + maxWordLength +
                " bytes.");
      }
      size++;
      seq.append(nextByte);
      return true;
    }
  }
}
