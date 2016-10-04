package com.hubspot.imap.utils.parsers.string;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.util.internal.AppendableCharSequence;

public abstract class BaseStringParser implements ByteBufProcessor {
  protected final SoftReferencedAppendableCharSequence sequenceReference;
  protected int size;

  protected AppendableCharSequence seq;

  public BaseStringParser(SoftReferencedAppendableCharSequence sequenceReference) {
    this.sequenceReference = sequenceReference;
  }

  public String parse(ByteBuf buffer) {
    seq = sequenceReference.get();

    seq.reset();
    size = 0;
    int i = buffer.forEachByte(this);
    buffer.readerIndex(i + 1);

    String result = seq.toString();
    seq = null;

    return result;
  }
}
