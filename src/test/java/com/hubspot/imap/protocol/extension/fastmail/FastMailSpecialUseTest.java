package com.hubspot.imap.protocol.extension.fastmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.hubspot.imap.ImapMultiServerTest;
import com.hubspot.imap.TestServerConfig;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.command.option.ReturnOption;
import com.hubspot.imap.protocol.command.option.SelectOption;
import com.hubspot.imap.protocol.folder.FolderMetadata;
import com.hubspot.imap.protocol.response.ResponseCode;
import com.hubspot.imap.protocol.response.tagged.ListResponse;

@RunWith(Parameterized.class)
public class FastMailSpecialUseTest extends ImapMultiServerTest {

  @Parameter
  public TestServerConfig testServerConfig;

  @Test
  public void itDoesGetSpecialUse_Select() throws Exception {
    if (!testServerConfig.imapConfiguration().hostAndPort().getHostText().contains("fastmail")) {
      return;
    }

    try (ImapClient client = getLoggedInClient(testServerConfig)) {
      ListResponse listResponse = client.extendedListWithSelectOption("", "*", SelectOption.SPECIAL_USE).get();
      assertThat(listResponse.getCode()).isEqualTo(ResponseCode.OK);
      assertThat(listResponse.getFolders().stream().map(FolderMetadata::getName).collect(Collectors.toList())).isNotEmpty();
    }
  }

  @Test
  public void itDoesGetSpecialUse_Return() throws Exception {
    if (!testServerConfig.imapConfiguration().hostAndPort().getHostText().contains("fastmail")) {
      return;
    }

    try (ImapClient client = getLoggedInClient(testServerConfig)) {
      ListResponse listResponse = client.extendedListWithReturnOption("", "%", ReturnOption.SPECIAL_USE).get();
      assertThat(listResponse.getCode()).isEqualTo(ResponseCode.OK);
      assertThat(listResponse.getFolders().stream().map(FolderMetadata::getName).collect(Collectors.toList()))
          .isNotEmpty();
    }
  }
}
