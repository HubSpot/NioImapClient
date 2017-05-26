package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.AuthenticationFailedException;

public class ImapClientAuthenticationTest extends BaseGreenMailServerTest {

  @Before
  public void setUp() throws Exception {
    super.setUp();
    deliverRandomMessage();
  }

  @Test
  public void testGivenInvalidCredentials_doesThrowAuthenticationException() throws Exception {
    try (ImapClient client = getClientFactory().connect().get()) {
      client.login(currentUser.getLogin(), "").await();
    } catch (ExecutionException e) {
      assertThat(e).hasCauseInstanceOf(AuthenticationFailedException.class);
    }
  }

}
