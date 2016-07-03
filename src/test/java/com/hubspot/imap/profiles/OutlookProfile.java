package com.hubspot.imap.profiles;

import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.ImapConfigurationIF.AuthType;
import com.hubspot.imap.utils.ImapServerDetails;

public class OutlookProfile extends EmailServerTestProfile {
  private static final String USER_NAME = "testing11235@outlook.com";
  private static final String PASSWORD = "***REMOVED***";
  private static final OutlookServerImplDetails OUTLOOK_SERVER_IMPL_DETAILS = new OutlookServerImplDetails();

  private static final ImapClientFactory OUTLOOK_CLIENT_FACTORY = new ImapClientFactory(
      ImapConfiguration.builder()
          .authType(AuthType.PASSWORD)
          .hostAndPort(ImapServerDetails.OUTLOOK.hostAndPort())
          .noopKeepAliveIntervalSec(10)
          .useEpoll(true)
          .build()
  );

  private static final OutlookProfile OUTLOOK_PROFILE = new OutlookProfile();

  public static OutlookProfile getOutlookProfile() {
    return OUTLOOK_PROFILE;
  }

  private OutlookProfile() {
  }

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

  @Override
  public String description() {
    return String.format("Outlook [%s]", USER_NAME);
  }

  private static class OutlookServerImplDetails implements EmailServerImplDetails {
    private static final String ALL_MAIL = "Inbox";

    private OutlookServerImplDetails() {
    }

    @Override
    public String getAllMailFolderName() {
      return ALL_MAIL;
    }
  }
}
