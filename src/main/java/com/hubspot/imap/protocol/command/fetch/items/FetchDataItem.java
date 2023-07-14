package com.hubspot.imap.protocol.command.fetch.items;

import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import java.util.Iterator;

public interface FetchDataItem {
  String toString();

  enum FetchDataItemType implements FetchDataItem {
    ALL("ALL"),
    FAST("FAST"),
    FULL("FULL"),
    FLAGS("FLAGS"),
    INTERNALDATE("INTERNALDATE"),
    ENVELOPE("ENVELOPE"),
    BODY("BODY"),
    BODYSTRUCTURE("BODYSTRUCTURE"),
    RFC822("RFC822"),
    RFC822_HEADER("RFC822.HEADER"),
    RFC822_SIZE("RFC822.SIZE"),
    RFC822_TEXT("RFC822.TEXT"),
    UID("UID"),
    X_GM_MSGID("X-GM-MSGID"),
    X_GM_THRID("X-GM-THRID"),
    X_GM_LABELS("X-GM-LABELS"),
    INVALID("----");

    private String string;

    FetchDataItemType(String string) {
      this.string = string;
    }

    public String toString() {
      return string;
    }

    public static FetchDataItemType getFetchType(String word) {
      TREE.getKeyValuePairsForClosestKeys(word);
      Iterator<KeyValuePair<FetchDataItemType>> responseType = TREE
        .getKeyValuePairsForClosestKeys(word)
        .iterator();
      if (!responseType.hasNext()) {
        return INVALID;
      }

      KeyValuePair<FetchDataItemType> candidate = responseType.next();

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
      for (FetchDataItemType type : FetchDataItemType.values()) {
        TREE.put(type.toString(), type);
        maxLength = Math.max(maxLength, type.toString().length());
      }

      MAX_PREFIX_LENGTH = maxLength;
    }
  }
}
