package com.hubspot.imap.utils.parsers;

import com.hubspot.imap.utils.NilMarker;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class NestedArrayParser<T> {

  private static final char LPAREN = '(';
  private static final char RPAREN = ')';

  private List<Object> values;
  private boolean foundLeftParen;
  private final ByteBufParser<T> itemParser;
  private final Recycler<T> recycler;
  private final io.netty.util.Recycler.Handle handle;

  public NestedArrayParser(ByteBufParser<T> itemParser, Recycler<T> recycler, io.netty.util.Recycler.Handle handle) {
    this.itemParser = itemParser;
    this.recycler = recycler;
    this.handle = handle;
  }

  public List<Object> parse(ByteBuf buffer) {
    values = new ArrayList<>();

    foundLeftParen = false;

    for (int i = 0; i < buffer.readableBytes(); i++) {
      char nextByte = ((char) buffer.readUnsignedByte());

      if (nextByte == LPAREN) {
        if (foundLeftParen) {
          buffer.readerIndex(buffer.readerIndex() - 1); // This is actually the start of the next array

          NestedArrayParser<T> nestedArrayParser = recycler.get();
          values.add(nestedArrayParser.parse(buffer));
          nestedArrayParser.recycle();
        } else {
          foundLeftParen = true;
        }
      } else if (nextByte == RPAREN) {
        return values;
      } else if (Character.isWhitespace(nextByte)) {
      } else if (nextByte == 'N') {
        int read = 2;
        nextByte = ((char) buffer.readUnsignedByte());
        if (nextByte == 'I') {
          nextByte = ((char) buffer.readUnsignedByte());
          read++;
          if (nextByte == 'L') {
            if (!foundLeftParen) {
              return values;
            } else {
              values.add(NilMarker.INSTANCE);
            }

            continue;
          }
        }

        buffer.readerIndex(buffer.readerIndex() - read);
        values.add(itemParser.parse(buffer));
      } else {
        buffer.readerIndex(buffer.readerIndex() - 1);
        values.add(itemParser.parse(buffer));
      }
    }

    return values;
  }

  public void recycle() {
    values = new ArrayList<>();
    foundLeftParen = false;
    recycler.recycle(this, handle);
  }

  public static class Recycler<X> extends io.netty.util.Recycler<NestedArrayParser<X>> {

    private final ByteBufParser<X> itemParser;

    public Recycler(ByteBufParser<X> itemParser) {
      this.itemParser = itemParser;
    }

    @Override
    protected NestedArrayParser<X> newObject(Handle handle) {
      return new NestedArrayParser<>(itemParser, this, handle);
    }
  }
}
