package com.hubspot.imap.protocol.extension.gmail.keys;

import com.hubspot.imap.protocol.command.search.keys.BaseSearchKey;
import com.hubspot.imap.protocol.extension.gmail.GmailSearchKeyTypes;

public class GmailMsgIdSearchKey extends BaseSearchKey {

  public GmailMsgIdSearchKey(long msgId) {
    super(GmailSearchKeyTypes.MSGID, String.valueOf(msgId));
  }
}
