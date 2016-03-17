package com.hubspot.imap.profiles;

import java.io.IOException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Throwables;
import com.hubspot.imap.ImapClientFactory;
import com.hubspot.imap.ImapConfiguration;
import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.utils.ImapServerDetails;
import org.assertj.core.util.Strings;

public class GmailOAuthProfile extends EmailServerTestProfile {

  private static final String USER_NAME = "hsimaptest1@gmail.com";

  private static final String APP_ID = null;
  private static final String APP_SECRET = null;
  private static final String REFRESH_TOKEN = null;

  private static final GmailServerImplDetails GMAIL_SERVER_IMPL_DETAILS = new GmailServerImplDetails();
  private static final ImapClientFactory GMAIL_CLIENT_FACTORY = new ImapClientFactory(
      new ImapConfiguration.Builder()
          .setAuthType(AuthType.XOAUTH2)
          .setHostAndPort(ImapServerDetails.GMAIL.hostAndPort())
          .setNoopKeepAliveIntervalSec(10)
          .setUseEpoll(true)
          .build()
  );

  private static GmailOAuthProfile GMAIL_PROFILE;

  public static GmailOAuthProfile getGmailProfile() {
    if (GMAIL_PROFILE == null) {
      GMAIL_PROFILE = new GmailOAuthProfile();
    }
    return GMAIL_PROFILE;
  }

  public static boolean shouldRun() {
    // Paste your App info in the static variables to add this test profile to the set to run
    return !Strings.isNullOrEmpty(APP_ID) && !Strings.isNullOrEmpty(APP_SECRET) && !Strings.isNullOrEmpty(REFRESH_TOKEN);
  }

  private final String accessToken;

  private GmailOAuthProfile() {
    GoogleCredential googleCredential = new GoogleCredential.Builder()
        .setClientSecrets(APP_ID, APP_SECRET)
        .setJsonFactory(new JacksonFactory())
        .setTransport(new NetHttpTransport())
        .build();

    googleCredential.setRefreshToken(REFRESH_TOKEN);

    try {
      googleCredential.refreshToken();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    accessToken = googleCredential.getAccessToken();
  }

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
    return accessToken;
  }

  @Override
  public String description() {
    return String.format("Gmail OAuth [%s]", USER_NAME);
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
