package com.hubspot.imap.imap.command;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.hubspot.java.utils.Bytes;

import java.util.List;

public class XOAuth2Command extends BaseCommand {
  private final static BaseEncoding B64 = BaseEncoding.base64();
  private final static String SASL_FORMAT = "user=%s\001auth=Bearer %s\001\001";
  private final static String MECHANISM = "XOAUTH2";

  private final String userName;
  private final String accessToken;

  public XOAuth2Command(String userName, String accessToken, int tag) {
    super(CommandType.AUTHENTICATE, tag);

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
    return B64.encode(Bytes.toBytes(String.format(SASL_FORMAT, userName, accessToken)));
  }
}
