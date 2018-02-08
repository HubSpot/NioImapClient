package com.hubspot.imap.utils.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnvelopeField {
  DATE("date"),
  SUBJECT("subject"),
  FROM("from"),
  SENDER("sender"),
  REPLY_TO("reply-to"),
  TO("to"),
  CC("cc"),
  BCC("bcc"),
  IN_REPLY_TO("in-reply-to"),
  MESSAGE_ID("message-id");

  private final String fieldName;
  public static final Map<String, EnvelopeField> NAME_INDEX = Arrays.stream(EnvelopeField.values()).collect(Collectors.toMap(EnvelopeField::getFieldName, Function.identity()));

  EnvelopeField(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldName() {
    return fieldName;
  }
}
