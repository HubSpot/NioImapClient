package com.hubspot.imap.imap.response;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

public class Response {
  private static final Splitter SPLITTER = Splitter.on(" ").limit(3).omitEmptyStrings().trimResults();
  private static final Splitter ANONYMOUS_SPLITTER = Splitter.on(" ").limit(2).omitEmptyStrings().trimResults();
  private static final String ANONYMOUS_ID = "*";

  private final boolean anonymous;
  private final String id;
  private final String contents;
  private final ResponseCode code;

  protected Response(boolean anonymous, String id, String contents, ResponseCode code) {
    this.anonymous = anonymous;
    this.id = id;
    this.contents = contents;
    this.code = code;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public String getId() {
    return id;
  }

  public String getContents() {
    return contents;
  }

  public boolean isOk() {
    return code == ResponseCode.OK;
  }

  public static Response parse(String input) throws IOException {
    if (input.startsWith(ANONYMOUS_ID)) {
      return parseAnonymous(input);
    } else {
      return parseCommandResponse(input);
    }
  }

  public static Response parseAnonymous(String input) throws IOException{
    List<String> parts = Lists.newArrayList(ANONYMOUS_SPLITTER.split(input));
    if (parts.size() != 2) {
      throw new IOException(String.format("Could not parse imap response: %s", input));
    }

    return new Response(true, ANONYMOUS_ID, parts.get(1), ResponseCode.NONE);
  }

  public static Response parseCommandResponse(String input) throws IOException {
    List<String> parts = Lists.newArrayList(SPLITTER.split(input));
    if (parts.size() != 3) {
      throw new IOException(String.format("Could not parse imap response: %s", input));
    }

    return new Response(false, parts.get(0), parts.get(2), ResponseCode.valueOf(parts.get(1)));
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("anonymous", anonymous)
        .add("id", id)
        .add("contents", contents)
        .add("code", code)
        .toString();
  }
}
