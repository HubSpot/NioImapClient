package com.hubspot.imap.protocol.command.fetch.items;

public class BodyFetchDataItem implements FetchDataItem {

  private final boolean peek;
  private final String section;

  public BodyFetchDataItem(boolean peek, String section) {
    this.peek = peek;
    this.section = section;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("BODY");
    if (peek) {
      sb.append(".PEEK");
    }

    sb.append("[").append(section).append("]");
    return sb.toString();
  }
}
