package com.hubspot.imap.utils.parsers;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.util.internal.AppendableCharSequence;

import java.util.ArrayList;
import java.util.List;

public class ArrayParser implements ByteBufProcessor {
  private static final char LPAREN = '(';
  private static final char RPAREN = ')';

  private final List<String> values;
  private final AppendableCharSequence seq;

  public ArrayParser(AppendableCharSequence seq) {
    this.values = new ArrayList<>();
    this.seq = seq;
  }

  public List<String> parse(ByteBuf buffer) {
    values.clear();
    seq.reset();
    int i = buffer.forEachByte(this);
    buffer.readerIndex(i + 1);
    return Lists.newArrayList(values);
  }

  @Override
  public boolean process(byte value) throws Exception {
    char nextByte = (char) value;
    if (Character.isWhitespace(nextByte)) {
      values.add(seq.toString());
      seq.reset();
      return true;
    } else if (nextByte == LPAREN) {
      return true;
    } else if (nextByte == RPAREN) {
      return false;
    } else {
      seq.append(nextByte);
      return true;
    }
  }
}
