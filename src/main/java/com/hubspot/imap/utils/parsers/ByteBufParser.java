package com.hubspot.imap.utils.parsers;

import io.netty.buffer.ByteBuf;

public interface ByteBufParser<T> {
  T parse(ByteBuf in);
}
