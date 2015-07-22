package com.hubspot.imap.protocol.response.tagged;

import com.hubspot.imap.protocol.exceptions.ResponseParseException;
import com.hubspot.imap.protocol.folder.FolderMetadata;

import java.util.List;
import java.util.stream.Collectors;

public interface ListResponse extends TaggedResponse {
  List<FolderMetadata> getFolders();

  class Builder extends TaggedResponse.Builder implements ListResponse {
    private List<FolderMetadata> folders;

    public ListResponse fromResponse(TaggedResponse input) throws ResponseParseException {
      folders = input.getUntagged().stream()
          .filter(o -> o instanceof FolderMetadata)
          .map(o -> ((FolderMetadata) o))
          .collect(Collectors.toList());

      setCode(input.getCode());
      setMessage(input.getMessage());
      setTag(input.getTag());

      return this;
    }

    public List<FolderMetadata> getFolders() {
      return folders;
    }
  }
}
