package com.hubspot.imap.utils.parsers.string;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;

import io.netty.handler.codec.TooLongFrameException;

public class WordParser extends BaseStringParser {
  private final int maxWordLength;

  public WordParser(SoftReferencedAppendableCharSequence seq, int maxWordLength) {
    super(seq);
    this.maxWordLength = maxWordLength;
  }

  @Override
  public boolean process(byte value) throws Exception {
    char nextByte = (char) value;
    if (Character.isWhitespace(nextByte)) {
      return size == 0;
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
