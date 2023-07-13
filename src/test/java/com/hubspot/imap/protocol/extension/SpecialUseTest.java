package com.hubspot.imap.protocol.extension;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.imap.ImapMultiServerTest;
import com.hubspot.imap.TestServerConfig;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.ListCommand;
import com.hubspot.imap.protocol.command.option.ReturnOption;
import com.hubspot.imap.protocol.folder.FolderMetadata;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.ListResponse;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public class SpecialUseTest extends ImapMultiServerTest {

  @Parameter
  public TestServerConfig testServerConfig;

  public void itDoesGetSpecialUse_Select() throws Exception {
    try (ImapClient client = getLoggedInClient(testServerConfig)) {
      Future<ListResponse> futureListResponse = client.send(
        new ListCommand("", "*", ReturnOption.SPECIAL_USE)
      );
      ListResponse listResponse = futureListResponse.get();
      assertThat(listResponse.getCode()).isEqualTo(ResponseCode.OK);
      assertThat(
        listResponse
          .getFolders()
          .stream()
          .map(FolderMetadata::getName)
          .collect(Collectors.toList())
      )
        .isNotEmpty();
    }
  }

  @Test
  public void itDoesGetSpecialUse_Return() throws Exception {
    try (ImapClient client = getLoggedInClient(testServerConfig)) {
      Future<ListResponse> futureListResponse = client.send(
        new ListCommand("", "%", ReturnOption.SPECIAL_USE)
      );
      ListResponse listResponse = futureListResponse.get();
      assertThat(listResponse.getCode()).isEqualTo(ResponseCode.OK);
      assertThat(
        listResponse
          .getFolders()
          .stream()
          .map(FolderMetadata::getName)
          .collect(Collectors.toList())
      )
        .isNotEmpty();
    }
  }
}
