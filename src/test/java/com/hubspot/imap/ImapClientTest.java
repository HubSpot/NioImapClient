package com.hubspot.imap;

import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.imap.response.Response;
import com.hubspot.imap.imap.response.ResponseCode;
import com.hubspot.imap.utils.GmailUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.*;
public class ImapClientTest {

  private static String USER_NAME = "hsimaptest1@gmail.com";
  private static String PASSWORD = "***REMOVED***";

  private ImapClientFactory clientFactory;

  @Before
  public void setUp() throws Exception {
    clientFactory = new ImapClientFactory(
        new ImapConfiguration.Builder()
            .setAuthType(AuthType.PASSWORD)
            .setHostAndPort(GmailUtils.GMAIL_HOST_PORT)
    );
  }
  @Test
  public void testCanLogin() throws Exception {
    ImapClient client = clientFactory.connect(USER_NAME, PASSWORD);

    client.login();
    client.awaitLogin();
    Future<Response> noopResponseFuture = client.noop();
    Response response = noopResponseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
  }

}
