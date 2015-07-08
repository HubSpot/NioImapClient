package com.hubspot.imap.imap.response;

import com.hubspot.imap.imap.exceptions.ResponseParseException;
import com.hubspot.imap.imap.folder.Folder;

import java.util.ArrayList;
import java.util.List;

public class ListResponse extends BaseResponse {
  private List<Folder> folders;

  @Override
  public Response fromRawResponse(RawResponse input) throws ResponseParseException {
    super.fromRawResponse(input);
    parseFolders(input.getUntaggedLines());

    return this;
  }

  private void parseFolders(List<String> untaggedResponses) throws ResponseParseException {
    List<Folder> folders = new ArrayList<>(untaggedResponses.size());
    for (String response: untaggedResponses) {
      folders.add(Folder.parseFromListResponse(response));
    }

    this.folders = folders;
  }

  public List<Folder> getFolders() {
    return folders;
  }
}
