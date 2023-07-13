package com.hubspot.imap;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.ImapCommandType;
import com.hubspot.imap.protocol.command.auth.AuthenticatePlainCommand;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.TaggedResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public class AuthenticationMechanismTest extends ImapMultiServerTest {

  @Parameter
  public TestServerConfig testServerConfig;

  @Test
  public void itDoesSuccesfullyAuthenticatePlain() throws Exception {
    try (ImapClient client = getClientForConfig(testServerConfig)) {
      TaggedResponse capResponse = client.send(ImapCommandType.CAPABILITY).join();

      client
        .send(
          new AuthenticatePlainCommand(
            client,
            testServerConfig.user(),
            testServerConfig.password()
          )
        )
        .join();
    }
  }

  @Test
  public void itDoesSuccesfullyChooseSuccessfulAuthMechanismOnLogin() throws Exception {
    try (ImapClient client = getClientForConfig(testServerConfig)) {
      TaggedResponse response = client
        .login(testServerConfig.user(), testServerConfig.password())
        .join();

      assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    }
  }
}
