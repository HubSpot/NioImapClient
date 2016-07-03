package com.hubspot.imap.profiles;

import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.ImapConfigurationIF.AuthType;
import com.hubspot.imap.utils.ImapServerDetails;

public class Outlook365Profile extends EmailServerTestProfile {
  private static final String USER_NAME = "eszabowexler@sidekick.engineering";
  private static final String PASSWORD = "***REMOVED***";
  private static final Outlook365ServerImplDetails OUTLOOK_365_SERVER_IMPL_DETAILS = new Outlook365ServerImplDetails();

  private static final ImapClientFactory OUTLOOK_CLIENT_FACTORY = new ImapClientFactory(
      ImapConfiguration.builder()
          .authType(AuthType.PASSWORD)
          .hostAndPort(ImapServerDetails.OUTLOOK_365.hostAndPort())
          .noopKeepAliveIntervalSec(10)
          .useEpoll(true)
          .build()
  );

  private static final Outlook365Profile OUTLOOK_365_PROFILE = new Outlook365Profile();
  public static Outlook365Profile getOutlook365Profile() {
    return OUTLOOK_365_PROFILE;
  }

  private Outlook365Profile() {}

  @Override
  public ImapClientFactory getClientFactory() {
    return OUTLOOK_CLIENT_FACTORY;
  }

  @Override
  public EmailServerImplDetails getImplDetails() {
    return OUTLOOK_365_SERVER_IMPL_DETAILS;
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
    return String.format("Outlook 365 [%s]", USER_NAME);
  }

  private static class Outlook365ServerImplDetails implements EmailServerImplDetails {
    private static final String ALL_MAIL = "INBOX";

    private Outlook365ServerImplDetails() {}

    @Override
    public String getAllMailFolderName() {
      return ALL_MAIL;
    }
  }
}
