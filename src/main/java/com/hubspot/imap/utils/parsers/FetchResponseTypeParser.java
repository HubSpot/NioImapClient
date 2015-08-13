package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class FetchResponseTypeParser implements ByteBufParser<String> {
  private final AppendableCharSequence seq;
  private final int maxWordLength;
  private int size = 0;

  public FetchResponseTypeParser(AppendableCharSequence seq, int maxWordLength) {
    this.seq = seq;
    this.maxWordLength = maxWordLength;
  }

  @Override
  public String parse(ByteBuf in) {
    seq.reset();
    for (;;) {
      char nextByte = (char) in.readUnsignedByte();
      if (Character.isWhitespace(nextByte)) {
        if (size > 0) {
          break;
        }
      } else if (!Character.isLetterOrDigit(nextByte) && nextByte != '.' && nextByte != '-') {
        in.readerIndex(in.readerIndex() - 1);
        break;
      } else {
        if (size >= maxWordLength) {
          throw new TooLongFrameException(
              "Word is larger than " + maxWordLength +
                  " bytes.");
        }
        size++;
        seq.append(nextByte);
      }
    }

    return seq.toString();
  }
}
