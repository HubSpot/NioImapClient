package com.hubspot.imap.imap.response;

import com.hubspot.imap.ImapClient;
import com.hubspot.imap.imap.exceptions.ResponseParseException;
import com.hubspot.imap.imap.folder.Folder;
import com.hubspot.imap.imap.folder.FolderMetadata;

import java.util.List;
import java.util.stream.Collectors;

public interface ListResponse extends Response {
  List<Folder> getFolders();

  class Builder extends Response.Builder implements ListResponse {
    private List<Folder> folders;

    public ListResponse fromResponse(Response input, ImapClient client) throws ResponseParseException {
      List<FolderMetadata> metadata = parseMetadata(input.getUntagged());
      folders = metadata.stream()
          .map(m -> new Folder(m, client))
          .collect(Collectors.toList());

      return this;
    }

    private List<FolderMetadata> parseMetadata(List<String> untaggedResponses) throws ResponseParseException {
      return untaggedResponses.stream()
          .map(r -> new FolderMetadata.Builder().parseFrom(r))
          .collect(Collectors.toList());
    }

    public List<Folder> getFolders() {
      return folders;
    }
  }
}
