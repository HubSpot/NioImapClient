NioImapClient [![Build Status](https://travis-ci.org/HubSpot/NioImapClient.svg?branch=master)](https://travis-ci.org/HubSpot/NioImapClient)
=============

High performance IMAP client in Java based on [Netty](https://netty.io/)

## Maven Dependency

```xml
<dependency>
  <groupId>com.hubspot</groupId>
  <artifactId>NioImapClient</artifactId>
  <version>0.4</version>
</dependency>
```

### Features

- High performance, designed to handle many concurrent connections.
- TLS supported out of the box
- `XOAUTH2` support
- Support for most GMail IMAP extensions 
- Implemented RFCs
  - [ ] RFC 3501 (not all commands supported, but its easy to add new ones, PRs always welcome!)
  - [x] RFC 2595 (TLS)
  - [x] RFC 6154 (Special-Use list)

### Limitations

- Many commands are still not implemented (most command related to appending/updating messages are not implemented)
- Pipelining of commands is not supported. It is hard to tell when the responses to these commands may be ambiguous, if you really need to execute concurrent commands we suggest simply opening multiple connections.
- The client currently provides no facilities for tracking message sequence numbers, we rely more heavily on UIDs.
- The server can send arbitrary untagged responses at any time, currently these get attached to the tagged response for the current command, this API needs improvement.

### Future plans

(in no particular order)

- [ ] RFC 5465 (`NOTIFY`)
- [ ] RFC 2177 (`IDLE`)
- [ ] RFC 2971 (`ID`)
- [ ] Sequence number tracking
- [ ] Full RFC 3501

### Notes For Developers

- NEVER execute blocking commands on an eventloop thread (i.e. `CountDownLatch.await` or `Future.get`)
  - Calls out to unknown functions (i.e. callbacks or event listeners) should always be done on isolated thread pools.
  - Slow but not necessarily blocking operations should also be avoided
- Attempt to avoid doing long running tasks on event loop threads
- Use `new` as sparingly as possible to avoid creating garbage:
  - Share objects when possible
  - Use netty bytebuf allocators when possible
  - Use netty recyclers for objects that can be recycled.
