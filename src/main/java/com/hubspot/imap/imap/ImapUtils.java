package com.hubspot.imap.imap;

import com.google.seventeen.common.base.Splitter;
import com.hubspot.imap.imap.exceptions.ResponseParseException;

import java.util.List;

public class ImapUtils {
  public static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

  public static List<String> parseImapList(String in) throws ResponseParseException {
    if (!in.startsWith("(") || !in.endsWith(")")) {
      throw new ResponseParseException("Could not parse list, not enclosed in parentheses");
    }

    return SPACE_SPLITTER.splitToList(in.subSequence(1, in.length()-1));
  }
}
