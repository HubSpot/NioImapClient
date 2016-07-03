package com.hubspot.imap.profiles;

import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.ImapConfigurationIF.AuthType;
import com.hubspot.imap.utils.ImapServerDetails;

public class GmailProfile extends EmailServerTestProfile {
  static final String USER_NAME = "hsimaptest1@gmail.com";
  private static final String PASSWORD = "***REMOVED***";
  private static final GmailServerImplDetails GMAIL_SERVER_IMPL_DETAILS = new GmailServerImplDetails();
  private static final ImapClientFactory GMAIL_CLIENT_FACTORY = new ImapClientFactory(
      ImapConfiguration.builder()
          .authType(AuthType.PASSWORD)
          .hostAndPort(ImapServerDetails.GMAIL.hostAndPort())
          .noopKeepAliveIntervalSec(10)
          .useEpoll(true)
          .build()
  );

  private static final GmailProfile GMAIL_PROFILE = new GmailProfile();
  public static GmailProfile getGmailProfile() {
    return GMAIL_PROFILE;
  }

  GmailProfile() {}

  @Override
  public ImapClientFactory getClientFactory() {
    return GMAIL_CLIENT_FACTORY;
  }

  @Override
  public EmailServerImplDetails getImplDetails() {
    return GMAIL_SERVER_IMPL_DETAILS;
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
    return String.format("Gmail [%s]", USER_NAME);
  }

  private static class GmailServerImplDetails implements EmailServerImplDetails {
    private static final String ALL_MAIL = "[Gmail]/All Mail";

    private GmailServerImplDetails() {}

    @Override
    public String getAllMailFolderName() {
      return ALL_MAIL;
    }
  }
}
