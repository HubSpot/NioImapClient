package com.hubspot.imap.protocol.extension.gmail;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;

public interface GMailLabel {
  String getLabel();

  enum SystemLabel implements GMailLabel {
    SENT("\\Sent"),
    INBOX("\\Inbox"),
    DRAFTS("\\Draft");

    private static final Map<String, SystemLabel> INDEX = Maps.uniqueIndex(
      Arrays.asList(SystemLabel.values()),
      SystemLabel::getLabel
    );

    private final String label;

    SystemLabel(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }
  }

  class StringLabel implements GMailLabel {

    private final String label;

    public StringLabel(String label) {
      this.label = label;
    }

    @Override
    public String getLabel() {
      return label;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StringLabel that = (StringLabel) o;
      return Objects.equal(getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getLabel());
    }
  }

  static GMailLabel get(String label) {
    if (label.startsWith("\\")) {
      label = label.substring(1);
    }

    if (SystemLabel.INDEX.containsKey(label)) {
      return SystemLabel.INDEX.get(label);
    }

    return new StringLabel(label);
  }
}
