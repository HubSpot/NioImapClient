package com.hubspot.imap.imap.response;

import com.hubspot.imap.imap.exceptions.ResponseParseException;
import com.hubspot.imap.imap.folder.Folder;

import java.util.List;
import java.util.stream.Collectors;

public interface ListResponse extends Response {
  List<Folder> getFolders();

  class Builder extends Response.Builder implements ListResponse {
    private List<Folder> folders;

    public ListResponse fromResponse(Response input) throws ResponseParseException {
      parseFolders(input.getUntagged());

      return this;
    }

    private void parseFolders(List<String> untaggedResponses) throws ResponseParseException {
      this.folders = untaggedResponses.stream()
          .map(r -> new Folder.Builder().parseFrom(r))
          .collect(Collectors.toList());
    }

    public List<Folder> getFolders() {
      return folders;
    }
  }
}
