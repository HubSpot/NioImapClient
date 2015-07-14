package com.hubspot.imap.imap.response.tagged;

import com.hubspot.imap.ImapClient;
import com.hubspot.imap.imap.folder.FolderFlags;
import com.hubspot.imap.imap.folder.FolderFlags.Flag;
import com.hubspot.imap.imap.response.untagged.UntaggedIntResponse;

import java.util.Set;

public interface OpenResponse extends TaggedResponse {
  long getExists();
  long getRecent();
  long getUidNext();
  long getUidValidity();
  long getHighestModSeq();
  Set<Flag> getFlags();
  Set<Flag> getPermanentFlags();

  class Builder extends TaggedResponse.Builder implements OpenResponse {
    private long exists;
    private long recent;
    private long uidNext;
    private long uidValidity;
    private long highestModSeq;
    Set<Flag> flags;
    Set<Flag> permanentFlags;

    public OpenResponse fromResponse(TaggedResponse response, ImapClient client) {
      for (Object o : response.getUntagged()) {
        if (o instanceof UntaggedIntResponse) {
          UntaggedIntResponse intResponse = ((UntaggedIntResponse) o);
          switch (intResponse.getType()) {
            case EXISTS:
              setExists(intResponse.getValue());
              break;
            case UIDVALIDITY:
              setUidValidity(intResponse.getValue());
              break;
            case RECENT:
              setRecent(intResponse.getValue());
              break;
            case UIDNEXT:
              setUidNext(intResponse.getValue());
              break;
            case HIGHESTMODSEQ:
              setHighestModSeq(intResponse.getValue());
              break;
          }
        } else if (o instanceof FolderFlags) {
          FolderFlags flags = ((FolderFlags) o);
          if (flags.isPermanent()) {
            setPermanentFlags(flags.getFlags());
          } else {
            setFlags(flags.getFlags());
          }
        }
      }

      setCode(response.getCode());
      setMessage(response.getMessage());
      setTag(response.getTag());

      return this;
    }

    public long getExists() {
      return this.exists;
    }

    public OpenResponse.Builder setExists(long exists) {
      this.exists = exists;
      return this;
    }

    public long getRecent() {
      return this.recent;
    }

    public OpenResponse.Builder setRecent(long recent) {
      this.recent = recent;
      return this;
    }

    public long getUidNext() {
      return this.uidNext;
    }

    public OpenResponse.Builder setUidNext(long uidNext) {
      this.uidNext = uidNext;
      return this;
    }

    public long getUidValidity() {
      return this.uidValidity;
    }

    public OpenResponse.Builder setUidValidity(long uidValidity) {
      this.uidValidity = uidValidity;
      return this;
    }

    public long getHighestModSeq() {
      return this.highestModSeq;
    }

    public OpenResponse.Builder setHighestModSeq(long highestModSeq) {
      this.highestModSeq = highestModSeq;
      return this;
    }

    public Set<Flag> getFlags() {
      return this.flags;
    }

    public OpenResponse.Builder setFlags(Set<Flag> flags) {
      this.flags = flags;
      return this;
    }

    public Set<Flag> getPermanentFlags() {
      return this.permanentFlags;
    }

    public OpenResponse.Builder setPermanentFlags(Set<Flag> permanentFlags) {
      this.permanentFlags = permanentFlags;
      return this;
    }

  }
}
