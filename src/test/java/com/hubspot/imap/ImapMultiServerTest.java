package com.hubspot.imap;

import com.hubspot.imap.profiles.EmailServerTestProfile;
import com.hubspot.imap.profiles.GmailProfile;
import com.hubspot.imap.profiles.Outlook365Profile;
import com.hubspot.imap.profiles.OutlookProfile;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.runners.Parameterized.Parameters;

public abstract class ImapMultiServerTest {
  protected static final List<EmailServerTestProfile> TEST_PROFILES = Arrays.asList(
    GmailProfile.getGmailProfile(),
    OutlookProfile.getOutlookProfile(),
    Outlook365Profile.getOutlook365Profile()
  );

  @Parameters(name="{0}")
  public static Collection<EmailServerTestProfile> parameters() {
    return TEST_PROFILES;
  }
}
