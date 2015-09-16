package com.hubspot.imap.protocol.response.untagged;

import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;

import java.util.Iterator;

public enum UntaggedResponseType {
  OK("OK"),
  BYE("BYE"),
  CAPABILITY("CAPABILITY"),
  LIST("LIST"),
  EXISTS("EXISTS"),
  EXPUNGE("EXPUNGE"),
  RECENT("RECENT"),
  UIDNEXT("[UIDNEXT"),
  UIDVALIDITY("[UIDVALIDITY"),
  PERMANENTFLAGS("[PERMANENTFLAGS"),
  HIGHESTMODSEQ("[HIGHESTMODSEQ"),
  FLAGS("FLAGS"),
  FETCH("FETCH"),
  SEARCH("SEARCH"),
  INVALID("-----");

  private final String prefix;

  UntaggedResponseType(String prefix) {
    this.prefix = prefix;
  }

  public static UntaggedResponseType getResponseType(String word) {
    TREE.getKeyValuePairsForClosestKeys(word);
    Iterator<KeyValuePair<UntaggedResponseType>> responseType = TREE.getKeyValuePairsForClosestKeys(word).iterator();
    if (!responseType.hasNext()) {
      return INVALID;
    }

    KeyValuePair<UntaggedResponseType> candidate = responseType.next();
    if (responseType.hasNext()) {
      return INVALID;
    }

    if (word.equalsIgnoreCase(candidate.getKey().toString())) {
      return candidate.getValue();
    } else {
      return INVALID;
    }
  }

  public static int getMaxPrefixLength() {
    return MAX_PREFIX_LENGTH;
  }

  private static final int MAX_PREFIX_LENGTH;
  private static final RadixTree<UntaggedResponseType> TREE;

  static {
    int maxLength = -1;
    TREE = new ConcurrentRadixTree<>(new DefaultByteArrayNodeFactory());
    for (UntaggedResponseType type: UntaggedResponseType.values()) {
      TREE.put(type.prefix, type);
      maxLength = Math.max(maxLength, type.prefix.length());
    }

    MAX_PREFIX_LENGTH = maxLength;
  }
}
