package com.hubspot.imap.imap.folder;

import com.hubspot.imap.ImapClient;

public class Folder {
  private final FolderMetadata metadata;
  private final ImapClient imapClient;

  public Folder(FolderMetadata metadata, ImapClient imapClient) {
    this.metadata = metadata;
    this.imapClient = imapClient;
  }

  public void open(Mode mode) {

  }

  public enum Mode {
    READ_WRITE,
    READ_ONLY;
  }
}
