package com.hubspot.imap.protocol.command;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class XOAuth2Command extends BaseImapCommand {
  private static final BaseEncoding B64 = BaseEncoding.base64();
  private static final String SASL_FORMAT = "user=%s\001auth=Bearer %s\001\001";
  private static final String MECHANISM = "XOAUTH2";

  private final String userName;
  private final String accessToken;

  public XOAuth2Command(String userName, String accessToken) {
    super(ImapCommandType.AUTHENTICATE);

    this.userName = userName;
    this.accessToken = accessToken;
  }

  @Override
  public List<String> getArgs() {
    return Lists.newArrayList(MECHANISM, getAuthenticateRequest());
  }

  @Override
  public boolean hasArgs() {
    return true;
  }

  public String getAuthenticateRequest() {
    return B64.encode(String.format(SASL_FORMAT, userName, accessToken).getBytes(StandardCharsets.UTF_8));
  }
}
