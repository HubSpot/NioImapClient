package com.hubspot.imap.protocol.folder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface FolderMetadata {
  List<FolderAttribute> getAttributes();
  String getContext();
  String getName();

  class Builder implements FolderMetadata {

    private final List<FolderAttribute> attributes = new ArrayList<>();
    private String context;
    private String name;

    public FolderMetadata build() {
      return this;
    }

    public List<FolderAttribute> getAttributes() {
      return this.attributes;
    }

    public Builder addAttribute(FolderAttribute attribute) {
      attributes.add(attribute);
      return this;
    }

    public Builder addAllAttributes(Collection<FolderAttribute> attributes) {
      this.attributes.addAll(attributes);
      return this;
    }

    public String getContext() {
      return this.context;
    }

    public Builder setContext(String context) {
      this.context = context;
      return this;
    }

    public String getName() {
      return this.name;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }
  }
}
