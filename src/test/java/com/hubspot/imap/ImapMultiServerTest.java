package com.hubspot.imap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.ConnectionClosedException;

public abstract class ImapMultiServerTest {
  private static List<TestServerConfig> getTestConfigs() throws IOException {
    InputStream inputStream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("profiles.yaml");

    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    return objectMapper.readValue(inputStream, new TypeReference<List<TestServerConfig>>() {});
  }

  @Parameters(name="{0}")
  public static Collection<TestServerConfig> parameters() throws IOException {
    try {
      return getTestConfigs();
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }


  protected static ImapClient getClientForConfig(TestServerConfig config) throws InterruptedException {
    ImapClientFactory clientFactory = new ImapClientFactory(config.imapConfiguration());
    return clientFactory.connect("test", config.user(), config.password());
  }

  protected static ImapClient getLoggedInClient(TestServerConfig config) throws InterruptedException, ExecutionException, ConnectionClosedException {
    ImapClient client = getClientForConfig(config);
    client.login();
    client.awaitLogin();

    return client;
  }
}
