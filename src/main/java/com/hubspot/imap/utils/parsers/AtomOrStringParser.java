package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class AtomOrStringParser implements ByteBufParser<String> {
  private static final char QUOTE = '"';
  private static final char BACKSLASH = '\\';

  protected final AppendableCharSequence seq;
  private final int maxStringLength;

  private int size;

  public AtomOrStringParser(AppendableCharSequence seq, int maxStringLength) {
    this.seq = seq;
    this.maxStringLength = maxStringLength;
  }

  public String parse(ByteBuf buffer) {
    seq.reset();
    size = 0;

    boolean isQuoted = false;
    char previousChar = ' ';
    for (;;) {
      char c;
      try {
        c = ((char) buffer.readUnsignedByte());
      } catch (IndexOutOfBoundsException e) {
        return seq.toString();
      }

      if (Character.isWhitespace(c)) {
        if (isQuoted) {
          append(c);
        } else if (size > 0) {
          break;
        }
      } else if (c == QUOTE && previousChar != BACKSLASH) {
        if (size == 0) {    // Start Quote
          isQuoted = true;
        } else {            // End Quote
          break;
        }
      } else {
        append(c);
      }

      previousChar = c;
    }

    return seq.toString();
  }

  private void append(char c) {
    if (size >= maxStringLength) {
      throw new TooLongFrameException("String is larger than " + maxStringLength + " bytes.");
    }

    size++;
    seq.append(c);
  }

}
