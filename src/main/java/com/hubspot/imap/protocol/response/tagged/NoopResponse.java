package com.hubspot.imap.protocol.response.tagged;

import com.hubspot.imap.protocol.response.untagged.UntaggedIntResponse;
import com.hubspot.imap.protocol.response.untagged.UntaggedResponseType;

public interface NoopResponse extends TaggedResponse {
  long getExists();

  class Builder extends TaggedResponse.Builder implements NoopResponse {

    private long exists;

    public NoopResponse fromResponse(TaggedResponse input) {
      copy(input);

      for (Object o : input.getUntagged()) {
        if (o instanceof UntaggedIntResponse) {
          UntaggedIntResponse intResponse = ((UntaggedIntResponse) o);
          if (intResponse.getType() == UntaggedResponseType.EXISTS) {
            setExists(intResponse.getValue());
          }
        }
      }

      return this;
    }

    public long getExists() {
      return this.exists;
    }

    public NoopResponse.Builder setExists(long exists) {
      this.exists = exists;
      return this;
    }
  }
}
