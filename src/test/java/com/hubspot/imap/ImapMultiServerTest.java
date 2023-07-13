package com.hubspot.imap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hubspot.imap.client.ImapClient;
import com.hubspot.imap.protocol.exceptions.ConnectionClosedException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ImapMultiServerTest {

  private static final Logger LOG = LoggerFactory.getLogger(ImapMultiServerTest.class);

  private static List<TestServerConfig> getTestConfigs() throws IOException {
    InputStream inputStream = Thread
      .currentThread()
      .getContextClassLoader()
      .getResourceAsStream("profiles.yaml");

    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    return objectMapper.readValue(
      inputStream,
      new TypeReference<List<TestServerConfig>>() {}
    );
  }

  @Parameters(name = "{0}")
  public static Collection<TestServerConfig> parameters() throws IOException {
    try {
      return getTestConfigs();
    } catch (Exception e) {
      LOG.error("Failed to load test configs!", e);
      return Collections.emptyList();
    }
  }

  protected static ImapClient getClientForConfig(TestServerConfig config)
    throws InterruptedException, ExecutionException {
    ImapClientFactory clientFactory = new ImapClientFactory();
    return clientFactory
      .connect("test", config.imapConfiguration().withTracingEnabled(true))
      .get();
  }

  protected static ImapClient getLoggedInClient(TestServerConfig config)
    throws InterruptedException, ExecutionException, ConnectionClosedException {
    ImapClient client = getClientForConfig(config);
    client.login(config.user(), config.password()).join();

    return client;
  }
}
