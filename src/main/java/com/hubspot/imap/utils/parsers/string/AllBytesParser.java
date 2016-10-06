package com.hubspot.imap.utils.parsers.string;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Signal;
import io.netty.util.internal.AppendableCharSequence;

public class AllBytesParser extends BaseStringParser {
  static final String REPLAY = ReplayingDecoder.class.getName().concat(".REPLAY");

  public AllBytesParser(SoftReferencedAppendableCharSequence sequenceReference) {
    super(sequenceReference);
  }

  @Override
  public boolean process(byte b) throws Exception {
    seq.append((char)(short) (b & 255));
    size++;

    return true;
  }

  @Override
  public String parse(ByteBuf buffer) {
    AppendableCharSequence sequence = sequenceReference.get();

    int readerIndex = buffer.readerIndex();
    try {
      super.parse(buffer);
    } catch (Signal e) {
      if (e.toString().equalsIgnoreCase(REPLAY)) {
        buffer.readerIndex(readerIndex + size);
        return sequence.toString();
      }

      throw e;
    }

    return sequence.toString();
  }
}
