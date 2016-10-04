package com.hubspot.imap.utils.parsers.string;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.ByteBufParser;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;

public class AtomOrStringParser implements ByteBufParser<String> {
  private static final char QUOTE = '"';
  private static final char BACKSLASH = '\\';

  protected final SoftReferencedAppendableCharSequence sequenceRef;
  private final int maxStringLength;

  private int size;

  public AtomOrStringParser(SoftReferencedAppendableCharSequence sequenceRef, int maxStringLength) {
    this.sequenceRef = sequenceRef;
    this.maxStringLength = maxStringLength;
  }

  public String parse(ByteBuf buffer) {
    AppendableCharSequence seq = sequenceRef.get();

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
          append(seq, c);
        } else if (size > 0) {
          break;
        }
      } else if (c == QUOTE && previousChar != BACKSLASH) {
        if (size == 0) {    // Start Quote
          isQuoted = true;
        } else {            // End Quote
          break;
        }
      } else if (!isQuoted && (c == ')' || c == '(')) {
        buffer.readerIndex(buffer.readerIndex() - 1);
        break;
      } else {
        append(seq, c);
      }

      // Always ignore any characters after a backslash
      if (previousChar != BACKSLASH) {
        previousChar = c;
      } else {
        previousChar = ' ';
      }
    }

    return seq.toString();
  }

  private void append(AppendableCharSequence seq, char c) {
    if (size >= maxStringLength) {
      throw new TooLongFrameException("String is larger than " + maxStringLength + " bytes.");
    }

    size++;
    seq.append(c);
  }

}
