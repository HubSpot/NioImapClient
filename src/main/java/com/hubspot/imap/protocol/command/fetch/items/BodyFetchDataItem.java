package com.hubspot.imap.protocol.command.fetch.items;

public class BodyFetchDataItem implements FetchDataItem {

  protected final String section;

  public BodyFetchDataItem(String section) {
    this.section = section;
  }

  @Override
  public String toString() {
    return "BODY[" + section + "]";
  }
}
