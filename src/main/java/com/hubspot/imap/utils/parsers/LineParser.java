package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.AppendableCharSequence;

// TODO: Stolen from SMTPProxy, move somewhere common
public class LineParser implements ByteBufProcessor {
  private final AppendableCharSequence seq;
  private final int maxLineLength;
  private int size;

  public LineParser(AppendableCharSequence seq, int maxLineLength) {
    this.seq = seq;
    this.maxLineLength = maxLineLength;
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
    if (nextByte == HttpConstants.CR) {
      return true;
    } else if (nextByte == HttpConstants.LF) {
      return false;
    } else {
      if (size >= maxLineLength) {
        throw new TooLongFrameException(
            "Line is larger than " + maxLineLength +
                " bytes.");
      }
      size++;
      seq.append(nextByte);
      return true;
    }
  }
}
