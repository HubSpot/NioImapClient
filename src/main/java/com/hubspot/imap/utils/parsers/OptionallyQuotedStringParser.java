package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class OptionallyQuotedStringParser extends BaseStringParser {
  private static final char QUOTE = '"';


  private final int maxStringLength;

  private boolean isQuoted = false;

  public OptionallyQuotedStringParser(AppendableCharSequence seq, int maxStringLength) {
    super(seq);
    this.maxStringLength = maxStringLength;
  }

  @Override
  public AppendableCharSequence parse(ByteBuf buffer) {
    isQuoted = false;
    return super.parse(buffer);
  }

  @Override
  public boolean process(byte value) throws Exception {
    char c = ((char) value);
    if (Character.isWhitespace(c)) {
      if (isQuoted) {
        append(c);
        return true;
      }

      return size == 0;
    } else if (c ==  QUOTE) {
      if (size == 0) {    // Start Quote
        isQuoted = true;
        return true;
      } else {            // End Quote
        return false;
      }
    } else {
      append(c);
      return true;
    }
  }

  private void append(char c) {
    if (size >= maxStringLength) {
      throw new TooLongFrameException("String is larger than " + maxStringLength + " bytes.");
    }

    size++;
    seq.append(c);
  }

}
