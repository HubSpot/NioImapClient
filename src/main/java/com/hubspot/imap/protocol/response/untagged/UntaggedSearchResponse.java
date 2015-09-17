package com.hubspot.imap.protocol.response.untagged;

import java.util.List;

public class UntaggedSearchResponse implements UntaggedResponse {

  private final List<Long> ids;

  public UntaggedSearchResponse(List<Long> ids) {
    this.ids = ids;
  }

  @Override
  public UntaggedResponseType getType() {
    return UntaggedResponseType.SEARCH;
  }

  @Override
  public String getMessage() {
    return null;
  }

  public List<Long> getIds() {
    return ids;
  }
}
