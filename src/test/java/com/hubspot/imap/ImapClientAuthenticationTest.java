package com.hubspot.imap;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.AuthenticationFailedException;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class ImapClientAuthenticationTest {

  @Test
  public void testGivenInvalidCredentials_doesThrowAuthenticationException() throws Exception {
    try (ImapClient client = TestUtils.CLIENT_FACTORY.connect(TestUtils.USER_NAME, "")) {
      client.login();
      client.awaitLogin();
    } catch (ExecutionException e) {
      assertThat(e).hasCauseInstanceOf(AuthenticationFailedException.class);
    }
  }
}
