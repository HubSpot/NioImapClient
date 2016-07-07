package com.hubspot.imap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized.Parameters;

import com.hubspot.imap.profiles.EmailServerTestProfile;
import com.hubspot.imap.profiles.GmailOAuthProfile;
import com.hubspot.imap.profiles.GmailProfile;
import com.hubspot.imap.profiles.Outlook365Profile;
import com.hubspot.imap.profiles.OutlookProfile;

public abstract class ImapMultiServerTest {
  protected static final List<EmailServerTestProfile> TEST_PROFILES = Arrays.asList(
    GmailProfile.getGmailProfile(),
    OutlookProfile.getOutlookProfile(),
    Outlook365Profile.getOutlook365Profile()
    //YahooProfile.getYahooProfile()
  );

  @Parameters(name="{0}")
  public static Collection<EmailServerTestProfile> parameters() {
    if (GmailOAuthProfile.shouldRun()) {
      List<EmailServerTestProfile> profiles = new ArrayList<>(TEST_PROFILES);
      profiles.add(GmailOAuthProfile.getGmailProfile());
      return profiles;
    } else {
      return TEST_PROFILES;
    }
  }
}
