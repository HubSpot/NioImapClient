package com.hubspot.imap.utils.parsers;

import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class FetchResponseTypeParser extends BaseStringParser {
  private final int maxWordLength;

  public FetchResponseTypeParser(AppendableCharSequence seq, int maxWordLength) {
    super(seq);
    this.maxWordLength = maxWordLength;
  }

  @Override
  public boolean process(byte value) throws Exception {
    char nextByte = (char) value;
    if (Character.isWhitespace(nextByte)) {
      return size == 0;
    } else if (!Character.isLetterOrDigit(nextByte) && nextByte != '.' && nextByte != '-') {
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
