package com.hubspot.imap.imap.response;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.List;

public class RawResponse {
  private final List<String> untaggedLines;
  private String tag;
  private ResponseCode responseCode;
  private String responseMessage;

  public RawResponse() {
    this.untaggedLines = Lists.newArrayList();
  }

  public List<String> getUntaggedLines() {
    return untaggedLines;
  }

  public void addUntaggedLine(String line) {
    untaggedLines.add(line);
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public ResponseCode getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(ResponseCode responseCode) {
    this.responseCode = responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("untaggedLines", untaggedLines)
        .add("tag", tag)
        .add("responseCode", responseCode)
        .add("responseMessage", responseMessage)
        .toString();
  }
}
