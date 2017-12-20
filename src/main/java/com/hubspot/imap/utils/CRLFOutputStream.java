package com.hubspot.imap.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CRLFOutputStream extends FilterOutputStream {

  private static final int CR = '\r';
  private static final int LF = '\n';

  private int previous = 0;

  public CRLFOutputStream(OutputStream out) {
    super(out);
  }

  public void write(int b) throws IOException {
    if (b == CR) {
      writeCRLF();
    } else if (b == LF && previous != CR) {
      writeCRLF();
    } else {
      out.write(b);
    }

    previous = b;
  }

  public void write(byte b[], int off, int len) throws IOException {
    int begin = off;
    int adjustedLen = len + off;

    for (int i = off; i < adjustedLen; i++) {
      int current = b[i];
      if (current == CR) {
        out.write(b, begin, i - begin);
        writeCRLF();
        begin = i + 1;
      } else if (current == LF) {
        if (previous != CR) {
          out.write(b, begin, i - begin);
          writeCRLF();
        }
        begin = i + 1;
      }

      previous = current;
    }
    if (adjustedLen > begin) {
      out.write(b, begin, adjustedLen - begin);
    }
  }

  private void writeCRLF() throws IOException {
    out.write(CR);
    out.write(LF);
  }
}
