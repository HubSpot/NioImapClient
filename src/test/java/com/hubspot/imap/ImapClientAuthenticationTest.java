package com.hubspot.imap;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.profiles.EmailServerTestProfile;
import com.hubspot.imap.protocol.exceptions.AuthenticationFailedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ImapClientAuthenticationTest extends ImapMultiServerTest {
  @Parameter public EmailServerTestProfile testProfile;

  @Test
  public void testGivenInvalidCredentials_doesThrowAuthenticationException() throws Exception {
    try (ImapClient client = testProfile.getClientFactory().connect(testProfile.getUsername(), "")) {
      client.login();
      client.awaitLogin();
    } catch (ExecutionException e) {
      assertThat(e).hasCauseInstanceOf(AuthenticationFailedException.class);
    }
  }
}
