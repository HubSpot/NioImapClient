package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.AuthenticationFailedException;

@RunWith(Parameterized.class)
public class ImapClientAuthenticationTest extends ImapMultiServerTest {
  @Parameter public TestServerConfig testServerConfig;

  @Test
  public void testGivenInvalidCredentials_doesThrowAuthenticationException() throws Exception {
    ImapClientFactory clientFactory = new ImapClientFactory(testServerConfig.imapConfiguration());
    try (ImapClient client = clientFactory.connect(testServerConfig.user(), "")) {
      client.login();
      client.awaitLogin();
    } catch (ExecutionException e) {
      assertThat(e).hasCauseInstanceOf(AuthenticationFailedException.class);
    }
  }
}
