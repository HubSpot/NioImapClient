package com.hubspot.imap.utils;

import com.hubspot.imap.ImapConfiguration;
import io.netty.util.internal.AppendableCharSequence;

import java.lang.ref.SoftReference;

public class SoftReferencedAppendableCharSequence {
  private SoftReference<AppendableCharSequence> reference;

  private final ImapConfiguration configuration;

  public SoftReferencedAppendableCharSequence(ImapConfiguration configuration) {
    this.configuration = configuration;
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
    return new AppendableCharSequence(configuration.getDefaultResponseBufferSize());
  }
}
