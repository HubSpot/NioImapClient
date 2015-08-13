package com.hubspot.imap.protocol.message;

import com.google.seventeen.common.base.Splitter;
import com.google.seventeen.common.base.Strings;
import com.google.seventeen.common.collect.ImmutableMultimap;
import com.google.seventeen.common.collect.Multimap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MimeMessage {
  Multimap<String, String> getHeaders();
  Collection<String> getHeaderValues(String header);
  Optional<String> getHeaderValue(String header);

  class Builder implements MimeMessage {
    private static final Splitter COLON_SPLITTER = Splitter.on(":").omitEmptyStrings().trimResults();

    private Multimap<String, String> headers;

    public Builder parseFrom(String input) throws IOException {
      BufferedReader reader = new BufferedReader(new StringReader(input));

      String line;
      ImmutableMultimap.Builder<String, String> multiMapBuilder = ImmutableMultimap.builder();
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (Strings.isNullOrEmpty(line)) {
          break;
        }

        List<String> items = COLON_SPLITTER.splitToList(line);

        multiMapBuilder.put(items.get(0), items.get(1));
      }

      headers = multiMapBuilder.build();
      return this;
    }

    @Override
    public Multimap<String, String> getHeaders() {
      return headers;
    }

    @Override
    public Collection<String> getHeaderValues(String header) {
      return headers.get(header);
    }

    @Override
    public Optional<String> getHeaderValue(String header) {
      Collection<String> values = headers.get(header);
      return values.isEmpty() ? Optional.<String>empty() : Optional.of(values.iterator().next());
    }

    public MimeMessage build() {
      return this;
    }
  }
}
