package com.hubspot.imap.utils;

import java.lang.ref.SoftReference;

import io.netty.util.internal.AppendableCharSequence;

public class SoftReferencedAppendableCharSequence {
  private SoftReference<AppendableCharSequence> reference;

  private final int defaultResponseBufferSize;

  public SoftReferencedAppendableCharSequence(int defaultResponseBufferSize) {
    this.defaultResponseBufferSize = defaultResponseBufferSize;
    this.reference = new SoftReference<>(newCharSeq());
  }

  public AppendableCharSequence get() {
    AppendableCharSequence sequence = reference.get();
    if (sequence == null) {
      sequence = newCharSeq();
    }

    reference = new SoftReference<>(sequence);

    return sequence;
  }

  private AppendableCharSequence newCharSeq() {
    return new AppendableCharSequence(defaultResponseBufferSize);
  }
}
