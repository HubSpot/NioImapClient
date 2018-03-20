package com.hubspot.imap.utils.parsers.string;

import java.nio.charset.StandardCharsets;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.ByteBufParser;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
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
        if (size == 0 && !isQuoted) {    // Start Quote
          isQuoted = true;
        } else {            // End Quote
          break;
        }
      } else if (!isQuoted && (c == ')' || c == '(')) {
        buffer.readerIndex(buffer.readerIndex() - 1);
        break;
      } else if (!isQuoted && (c == '{')) {
        return parseLiteral(buffer, seq);
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

  /**
   * See https://tools.ietf.org/html/rfc3501.html#section-4.3
   *
   * String literals have the form
   *
   * {10}
   * abcdefghij
   *
   * Where {10} represents the length of the string literal. The literal
   * begins after any CLRF characters following the '}' character.
   */
  private String parseLiteral(ByteBuf buffer, AppendableCharSequence seq) {
    int length = 0;

    char c = (char) buffer.readUnsignedByte();
    while (c != '}') {
      if (Character.isDigit(c)) {
        int digit = Character.digit(c, 10);
        length = (length * 10) + digit;
        c = (char) buffer.readUnsignedByte();
      } else {
        throw new DecoderException(
            String.format("Found non-digit character %c where a digit was expected", c)
        );
      }
    }

    if (length > 0) {
      //ignore crlf characters after '}'
      c = (char) buffer.readUnsignedByte();
      while (isCRLFCharacter(c)) {
        c = (char) buffer.readUnsignedByte();
      }

      append(seq, c);
      append(seq, buffer, length);

      return seq.toString();
    } else {
      return "";
    }
  }

  private boolean isCRLFCharacter(char c) {
    return c == '\n' || c == '\r';
  }

  private void append(AppendableCharSequence seq, char c) {
    if (size >= maxStringLength) {
      throw new TooLongFrameException("String is larger than " + maxStringLength + " bytes.");
    }

    size++;
    seq.append(c);
  }

  private void append(AppendableCharSequence seq, ByteBuf buffer, int length) {
    if (size + length >= maxStringLength) {
      throw new TooLongFrameException("String is larger than " + maxStringLength + " bytes.");
    }

    size += length;
    seq.append(buffer.readCharSequence(length - 1, StandardCharsets.UTF_8));
  }
}
