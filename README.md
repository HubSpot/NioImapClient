NioImapClient [![Build Status](https://private.hubteam.com/jenkins/buildStatus/icon?job=NioImapClient)](https://private.hubteam.com/jenkins/job/NioImapClient)
=============

A better way to IMAP.

### Limitations

- Many commands are still not implemented
- You cannot FETCH item types that don't explicitly have parsers, doing so will throw an exception and kill your connection
- Keeping many concurrent connections open for the same account is bad (gmail limits us to 15)

### Notes For Developers

- NEVER execute blocking commands on an eventloop thread (i.e. `CountDownLatch.await` or `Future.get`)
- Attempt to avaoid doing long running tasks on event loop threads
- Use `new` as sparingly as possible:
  - Share objects when possible
  - Use netty bytebuf allocators when possible
