package com.hubspot.imap.utils.parsers;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class FetchResponseTypeParser implements ByteBufParser<String> {
  private final SoftReferencedAppendableCharSequence sequenceRef;
  private final int maxWordLength;
  private int size = 0;

  public FetchResponseTypeParser(SoftReferencedAppendableCharSequence sequenceRef, int maxWordLength) {
    this.sequenceRef = sequenceRef;
    this.maxWordLength = maxWordLength;
  }

  @Override
  public String parse(ByteBuf in) {
    AppendableCharSequence seq = sequenceRef.get();

    seq.reset();
    size = 0;
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
