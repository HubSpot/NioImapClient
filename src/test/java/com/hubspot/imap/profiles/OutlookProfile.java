package com.hubspot.imap.profiles;

import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.utils.OutlookUtils;

public class OutlookProfile implements EmailServerTestProfile {
  private static final String USER_NAME = "";
  private static final String PASSWORD = "";
  private static final OutlookServerImplDetails OUTLOOK_SERVER_IMPL_DETAILS = new OutlookServerImplDetails();

  private static final ImapClientFactory OUTLOOK_CLIENT_FACTORY = new ImapClientFactory(
    new ImapConfiguration.Builder()
      .setAuthType(AuthType.PASSWORD)
      .setHostAndPort(OutlookUtils.OUTLOOK_HOST_PORT)
      .setNoopKeepAliveIntervalSec(10)
      .setUseEpoll(true)
      .build()
  );

  private static final OutlookProfile OUTLOOK_PROFILE = new OutlookProfile();
  public static OutlookProfile getOutlookProfile() {
    return OUTLOOK_PROFILE;
  }

  private OutlookProfile() {}

  @Override
  public ImapClientFactory getClientFactory() {
    return OUTLOOK_CLIENT_FACTORY;
  }

  @Override
  public EmailServerImplDetails getImplDetails() {
    return OUTLOOK_SERVER_IMPL_DETAILS;
  }

  @Override
  public String getUsername() {
    return USER_NAME;
  }

  @Override
  public String getPassword() {
    return PASSWORD;
  }

  private static class OutlookServerImplDetails implements EmailServerImplDetails {
    private static final String ALL_MAIL = "[Outlook]/All Mail";

    private OutlookServerImplDetails() {}

    @Override
    public String getAllMailFolderName() {
      return ALL_MAIL;
    }
  }
}
