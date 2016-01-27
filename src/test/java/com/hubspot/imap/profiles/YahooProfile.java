package com.hubspot.imap.profiles;

import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.utils.ImapServerDetails;

public class YahooProfile extends EmailServerTestProfile {
  private static final String USER_NAME = "testing11235@yahoo.com";
  private static final String PASSWORD = "***REMOVED***";
  private static final YahooServerImplDetails YAHOO_SERVER_IMPL_DETAILS = new YahooServerImplDetails();

  private static final ImapClientFactory YAHOO_CLIENT_FACTORY = new ImapClientFactory(
    new ImapConfiguration.Builder()
      .setAuthType(AuthType.PASSWORD)
      .setHostAndPort(ImapServerDetails.YAHOO.hostAndPort())
      .setNoopKeepAliveIntervalSec(10)
      .setUseEpoll(true)
      .build()
  );

  private static final YahooProfile YAHOO_PROFILE = new YahooProfile();
  public static YahooProfile getYahooProfile() {
    return YAHOO_PROFILE;
  }

  private YahooProfile() {}

  @Override
  public ImapClientFactory getClientFactory() {
    return YAHOO_CLIENT_FACTORY;
  }

  @Override
  public EmailServerImplDetails getImplDetails() {
    return YAHOO_SERVER_IMPL_DETAILS;
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
    return String.format("Yahoo [%s]", USER_NAME);
  }

  private static class YahooServerImplDetails implements EmailServerImplDetails {
    private static final String ALL_MAIL = "Inbox";

    private YahooServerImplDetails() {}

    @Override
    public String getAllMailFolderName() {
      return ALL_MAIL;
    }
  }
}
