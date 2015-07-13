package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.util.internal.AppendableCharSequence;

public abstract class BaseStringParser implements ByteBufProcessor {
  protected final AppendableCharSequence seq;
  protected int size;

  public BaseStringParser(AppendableCharSequence seq) {
    this.seq = seq;
  }

  public AppendableCharSequence parse(ByteBuf buffer) {
    seq.reset();
    size = 0;
    int i = buffer.forEachByte(this);
    buffer.readerIndex(i + 1);
    return seq;
  }
}
