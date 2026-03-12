# Static-RSA Cipher Suite Integration Test

This directory contains a simulation IMAPS server that only accepts static-RSA
key exchange cipher suites (e.g. `TLS_RSA_WITH_AES_128_CBC_SHA`). These are the
cipher suites that JDK 21.0.10 disabled by default, breaking connections to
older mail servers that don't support ECDHE key exchange.

The integration test `StaticRsaCipherSuiteIT` validates that the
`sslCipherSuites` configuration option successfully enables these legacy suites.

## Files

- `simulate_old_imaps.py` — Python 3 server that speaks minimal IMAP4rev1 over
  TLS, restricted to static-RSA cipher suites only.
- `server.crt` / `server.key` — Self-signed certificate for the simulation
  server. Regenerate with:
  ```bash
  openssl req -x509 -newkey rsa:2048 -keyout server.key -out server.crt \
      -days 365 -nodes -subj "/CN=test-old-imap"
  ```

## Running the integration test

1. Start the simulation server:
   ```bash
   cd src/test/resources/static-rsa-simulation
   python3 simulate_old_imaps.py --port 10993
   ```

2. Run the integration tests:
   ```bash
   mvn verify -Pintegration-tests
   ```

   Or run just the single test:
   ```bash
   mvn verify -Pintegration-tests -Dit.test=StaticRsaCipherSuiteIT
   ```

## What the test verifies

The test connects to the RSA-only simulation server on `localhost:10993` with
`sslCipherSuites` set to static-RSA suites. Without this option, the connection
would fail on JDK versions that disable static-RSA key exchange (JDK 21.0.10+).
