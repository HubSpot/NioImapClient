package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

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
    try (ImapClient client = getClientFactory().connect(getImapConfig()).get()) {
      client.login(currentUser.getLogin(), "").join();
    } catch (Exception e) {
      assertThat(e).hasCauseInstanceOf(AuthenticationFailedException.class);
    }
  }

}
