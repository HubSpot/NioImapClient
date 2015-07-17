package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;

public class MatchingParenthesesParser implements ByteBufProcessor {
  private static final char LPAREN = '(';
  private static final char RPAREN = ')';

  protected ByteBuf byteBuf;
  protected int depth;

  public MatchingParenthesesParser() {
  }

  public ByteBuf parse(ByteBuf buffer, ByteBufAllocator allocator) {
    byteBuf = allocator.buffer();
    depth = 0;
    int i = buffer.forEachByte(this);
    buffer.readerIndex(i + 1);
    return byteBuf;
  }

  @Override
  public boolean process(byte value) throws Exception {
    char c = ((char) value);
    if (c == LPAREN) {
      depth++;

      if (depth == 1) {
        return true;
      } else {
        byteBuf.writeByte(value);
      }
    } else if (c == RPAREN) {
      depth--;
      if (depth == 0) {
        return false;
      } else {
        byteBuf.writeByte(value);
      }
    } else {
      byteBuf.writeByte(value);
    }

    return true;
  }
}
