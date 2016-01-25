package com.hubspot.imap;

import com.hubspot.imap.profiles.EmailServerTestProfile;
import com.hubspot.imap.profiles.GmailProfile;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.runners.Parameterized.Parameters;

public abstract class ImapMultiServerTest {
  protected static final List<EmailServerTestProfile> TEST_PROFILES = Arrays.asList(
    GmailProfile.getGmailProfile()
//      OutlookProfile.getOutlookProfile()
  );

  @Parameters
  public static Collection<EmailServerTestProfile> parameters() {
    return TEST_PROFILES;
  }
}
