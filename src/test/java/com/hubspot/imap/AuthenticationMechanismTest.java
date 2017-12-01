package com.hubspot.imap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.auth.AuthenticatePlainCommand;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;


@RunWith(Parameterized.class)
public class AuthenticationMechanismTest extends ImapMultiServerTest {
  @Parameter
  public TestServerConfig testServerConfig;


  @Test
  public void itDoesSuccesfullyAuthenticatePlain() throws Exception {
    try (ImapClient client = getClientForConfig(testServerConfig)) {
      TaggedResponse capResponse = client.send(ImapCommandType.CAPABILITY).join();

      client.send(new AuthenticatePlainCommand(client, testServerConfig.user(), testServerConfig.password())).join();
    }
  }
}
