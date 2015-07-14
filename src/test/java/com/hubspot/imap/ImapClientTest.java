package com.hubspot.imap;

import com.hubspot.imap.ImapConfiguration.AuthType;
import com.hubspot.imap.imap.exceptions.AuthenticationFailedException;
import com.hubspot.imap.imap.folder.FolderMetadata;
import com.hubspot.imap.imap.response.ResponseCode;
import com.hubspot.imap.imap.response.tagged.ListResponse;
import com.hubspot.imap.imap.response.tagged.OpenResponse;
import com.hubspot.imap.imap.response.tagged.TaggedResponse;
import com.hubspot.imap.utils.GmailUtils;
import io.netty.util.concurrent.Future;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

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
  public void testLogin_doesAuthenticateConnection() throws Exception {
    ImapClient client = getLoggedInClient();

    Future<TaggedResponse> noopResponseFuture = client.noop();
    TaggedResponse taggedResponse = noopResponseFuture.get();

    assertThat(taggedResponse.getCode()).isEqualTo(ResponseCode.OK);
  }

  @Test
  public void testList_doesReturnFolders() throws Exception {
    ImapClient client = getLoggedInClient();

    Future<ListResponse> listResponseFuture = client.list("", "[Gmail]/%");

    ListResponse response = listResponseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getFolders().size()).isGreaterThan(0);
    assertThat(response.getFolders()).have(new Condition<>(m -> m.getAttributes().size() > 0, "attributes"));
    assertThat(response.getFolders()).extracting(FolderMetadata::getName).contains("[Gmail]/All Mail");
  }

  @Test
  public void testGivenFolderName_canOpenFolder() throws Exception {
    ImapClient client = getLoggedInClient();

    Future<OpenResponse> responseFuture = client.open("[Gmail]/All Mail", false);
    OpenResponse response = responseFuture.get();

    assertThat(response.getCode()).isEqualTo(ResponseCode.OK);
    assertThat(response.getExists()).isGreaterThan(0);
    assertThat(response.getFlags().size()).isGreaterThan(0);
    assertThat(response.getPermanentFlags().size()).isGreaterThan(0);
    assertThat(response.getUidValidity()).isGreaterThan(0);
    assertThat(response.getRecent()).isEqualTo(0);
    assertThat(response.getUidNext()).isGreaterThan(0);
    assertThat(response.getHighestModSeq()).isGreaterThan(0);
  }

  @Test
  public void testGivenInvalidCredentials_doesThrowAuthenticationException() throws Exception {
    ImapClient client = clientFactory.connect(USER_NAME, "");
    try {
      client.login();
      client.awaitLogin();
    } catch (ExecutionException e) {
      assertThat(e).hasCauseInstanceOf(AuthenticationFailedException.class);
    }
  }

  private ImapClient getLoggedInClient() throws ExecutionException, InterruptedException {
    ImapClient client = clientFactory.connect(USER_NAME, PASSWORD);

    client.login();
    client.awaitLogin();

    return client;
  }

}
