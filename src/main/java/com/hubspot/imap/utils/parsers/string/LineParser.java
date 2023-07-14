package com.hubspot.imap.utils.parsers.string;

import com.hubspot.imap.utils.SoftReferencedAppendableCharSequence;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpConstants;

// TODO: Stolen from SMTPProxy, move somewhere common
public class LineParser extends BaseStringParser {

  private final int maxLineLength;

  public LineParser(SoftReferencedAppendableCharSequence seq, int maxLineLength) {
    super(seq);
    this.maxLineLength = maxLineLength;
  }

  @Override
  public boolean process(byte value) throws Exception {
    char nextByte = (char) value;
    if (nextByte == HttpConstants.CR) {
      return true;
    } else if (nextByte == HttpConstants.LF) {
      return false;
    } else {
      if (size >= maxLineLength) {
        throw new TooLongFrameException(
          "Line is larger than " + maxLineLength + " bytes."
        );
      }
      size++;
      seq.append(nextByte);
      return true;
    }
  }
}
