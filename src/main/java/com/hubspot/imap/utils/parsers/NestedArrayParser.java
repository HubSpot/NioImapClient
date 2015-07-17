package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.AppendableCharSequence;

import java.util.ArrayList;
import java.util.List;

public class NestedArrayParser<T> {
  private static final char LPAREN = '(';
  private static final char RPAREN = ')';

  private List<Object> values;
  private boolean foundLeftParen;
  private final AppendableCharSequence seq;
  private final ByteBufParser<T> itemParser;

  public NestedArrayParser(AppendableCharSequence seq, ByteBufParser<T> itemParser) {
    this.seq = seq;
    this.itemParser = itemParser;
  }

  public List<Object> parse(ByteBuf buffer) {
    values = new ArrayList<>();
    seq.reset();

    foundLeftParen = false;

    for (int i = 0; i < buffer.readableBytes(); i++) {
      char nextByte = ((char) buffer.readUnsignedByte());

      if (nextByte == LPAREN) {
        if (foundLeftParen) {
          buffer.readerIndex(buffer.readerIndex() - 1); // This is actually the start of the next array
          values.add(new NestedArrayParser<>(seq, itemParser).parse(buffer));
        } else {
          foundLeftParen = true;
        }
      } else if (nextByte == RPAREN) {
        break;
      } else if (Character.isWhitespace(nextByte) && !foundLeftParen) {
      } else {
        buffer.readerIndex(buffer.readerIndex() - 1);
        values.add(itemParser.parse(buffer));
      }
    }

    return values;
  }
}
