package com.hubspot.imap.utils.parsers.string;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import com.hubspot.imap.utils.parsers.ByteBufParser;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.AppendableCharSequence;

class LiteralStringSizeParser implements ByteBufParser<Integer> {

  SoftReferencedAppendableCharSequence sequenceRef;

  public LiteralStringSizeParser(SoftReferencedAppendableCharSequence sequenceRef) {
    this.sequenceRef = sequenceRef;
  }

  @Override
  public Integer parse(ByteBuf in) {
    AppendableCharSequence seq = sequenceRef.get();

    seq.reset();
    boolean foundStart = false;

    for (;;) {
      char c = ((char) in.readUnsignedByte());

      if (c == '{') {
        foundStart = true;
      } else if (c == '}') {
        return Integer.parseInt(seq.toString());
      } else if (foundStart) {
        seq.append(c);
      }
    }
  }
}
