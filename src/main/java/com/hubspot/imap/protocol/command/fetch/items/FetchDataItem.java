package com.hubspot.imap.protocol.command.fetch.items;

import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;

import java.util.Iterator;

public interface FetchDataItem {
  String toString();

  enum FetchDataItemType implements FetchDataItem {
    ALL,
    FAST,
    FULL,
    FLAGS,
    INTERNALDATE,
    ENVELOPE,
    BODY,
    BODY_PEEK,
    BODYSTRUCTURE,
    RFC822,
    RFC822_HEADER,
    RFC822_SIZE,
    RFC822_TEXT,
    UID,
    INVALID;

    private String string;

    FetchDataItemType() {
      string = name().replace("_", ".");
    }

    public String toString() {
      return string;
    }

    public static FetchDataItemType getFetchType(String word) {
      TREE.getKeyValuePairsForClosestKeys(word);
      Iterator<KeyValuePair<FetchDataItemType>> responseType = TREE.getKeyValuePairsForClosestKeys(word).iterator();
      if (!responseType.hasNext()) {
        return INVALID;
      }

      KeyValuePair<FetchDataItemType> candidate = responseType.next();
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
    private static final RadixTree<FetchDataItemType> TREE;

    static {
      int maxLength = -1;
      TREE = new ConcurrentRadixTree<>(new DefaultByteArrayNodeFactory());
      for (FetchDataItemType type: FetchDataItemType.values()) {
        TREE.put(type.toString(), type);
        maxLength = Math.max(maxLength, type.toString().length());
      }

      MAX_PREFIX_LENGTH = maxLength;
    }
  }
}
