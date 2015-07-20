package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class NestedArrayParser<T> {
  private static final char LPAREN = '(';
  private static final char RPAREN = ')';

  private List<Object> values;
  private boolean foundLeftParen;
  private final ByteBufParser<T> itemParser;

  public NestedArrayParser(ByteBufParser<T> itemParser) {
    this.itemParser = itemParser;
  }

  public List<Object> parse(ByteBuf buffer) {
    values = new ArrayList<>();

    foundLeftParen = false;

    for (int i = 0; i < buffer.readableBytes(); i++) {
      char nextByte = ((char) buffer.readUnsignedByte());

      if (nextByte == LPAREN) {
        if (foundLeftParen) {
          buffer.readerIndex(buffer.readerIndex() - 1); // This is actually the start of the next array
          values.add(new NestedArrayParser<>(itemParser).parse(buffer));
        } else {
          foundLeftParen = true;
        }
      } else if (nextByte == RPAREN) {
        return values;
      } else if (Character.isWhitespace(nextByte)) {
      } else if (nextByte == 'N' && !foundLeftParen) {
        // NIL, read the remaining 2 bytes and return an empty list;
        buffer.readBytes(2);
        return values;
      } else {
        buffer.readerIndex(buffer.readerIndex() - 1);
        values.add(itemParser.parse(buffer));
      }
    }

    return values;
  }
}
