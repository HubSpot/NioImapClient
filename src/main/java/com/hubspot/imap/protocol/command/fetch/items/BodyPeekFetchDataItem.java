package com.hubspot.imap.protocol.command.fetch.items;

public class BodyPeekFetchDataItem extends BodyFetchDataItem {

  public BodyPeekFetchDataItem(String section) {
    super(section);
  }

  public BodyPeekFetchDataItem() {
    super();
  }

  @Override
  public String toString() {
    return "BODY.PEEK[" + section + "]";
  }
}
