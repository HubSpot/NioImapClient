#!/usr/bin/env python3
"""
Simulates an old IMAPS server that only supports static-RSA key exchange
cipher suites — the kind that JDK 21.0.10 disabled by default.

Usage:
    # 1. Generate a self-signed RSA cert (one-time):
    openssl req -x509 -newkey rsa:2048 -keyout server.key -out server.crt \
        -days 365 -nodes -subj "/CN=test-old-imap"

    # 2. Run the server:
    python3 simulate_old_imaps.py --port 10993

    # 3. Verify with openssl that only RSA key exchange is offered:
    openssl s_client -connect localhost:10993 -cipher 'AES128-SHA'
    #   ^ should connect
    openssl s_client -connect localhost:10993 -cipher 'ECDHE-RSA-AES128-GCM-SHA256'
    #   ^ should fail with handshake_failure

    # 4. Point your facsimile QA config at localhost:10993 and confirm the
    #    SSLHandshakeException reproduces (and that your fix resolves it).
"""

import argparse
import socket
import ssl
import sys
import threading


# Static-RSA cipher suites only — no ECDHE, no DHE.
# These are the suites that old mail servers typically support and that
# JDK 21.0.10 removed from the default enabled set.
RSA_ONLY_CIPHERS = ":".join([
    "AES128-SHA",            # TLS_RSA_WITH_AES_128_CBC_SHA
    "AES256-SHA",            # TLS_RSA_WITH_AES_256_CBC_SHA
    "AES128-SHA256",         # TLS_RSA_WITH_AES_128_CBC_SHA256
    "AES256-SHA256",         # TLS_RSA_WITH_AES_256_CBC_SHA256
    "AES128-GCM-SHA256",     # TLS_RSA_WITH_AES_128_GCM_SHA256
    "AES256-GCM-SHA384",     # TLS_RSA_WITH_AES_256_GCM_SHA384
])


def build_ssl_context(certfile, keyfile, max_tls_version=None):
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.load_cert_chain(certfile, keyfile)

    # Only allow TLSv1.2 (or optionally cap lower to also test TLSv1.0-only)
    ctx.minimum_version = ssl.TLSVersion.TLSv1
    if max_tls_version == "1.0":
        ctx.maximum_version = ssl.TLSVersion.TLSv1
    elif max_tls_version == "1.1":
        ctx.maximum_version = ssl.TLSVersion.TLSv1_1
    else:
        ctx.maximum_version = ssl.TLSVersion.TLSv1_2

    ctx.set_ciphers(RSA_ONLY_CIPHERS)
    return ctx


IMAP_GREETING = b"* OK [CAPABILITY IMAP4rev1 AUTH=PLAIN] Test IMAP server ready\r\n"
IMAP_BYE = b"* BYE Server closing connection\r\n"


def handle_client(conn, addr):
    print(f"[+] TLS handshake succeeded from {addr}")
    try:
        conn.sendall(IMAP_GREETING)
        while True:
            data = conn.recv(4096)
            if not data:
                break
            line = data.decode("utf-8", errors="replace").strip()
            print(f"    <- {line}")
            tag = line.split()[0] if line.split() else "*"

            if line.upper().endswith("LOGOUT"):
                conn.sendall(IMAP_BYE)
                conn.sendall(f"{tag} OK LOGOUT completed\r\n".encode())
                break
            elif line.upper().endswith("CAPABILITY"):
                conn.sendall(b"* CAPABILITY IMAP4rev1 AUTH=PLAIN\r\n")
                conn.sendall(f"{tag} OK CAPABILITY completed\r\n".encode())
            elif "LOGIN" in line.upper():
                conn.sendall(f"{tag} OK LOGIN completed\r\n".encode())
            elif "SELECT" in line.upper() or "EXAMINE" in line.upper():
                conn.sendall(b"* 0 EXISTS\r\n* 0 RECENT\r\n")
                conn.sendall(f"{tag} OK SELECT completed\r\n".encode())
            else:
                conn.sendall(f"{tag} BAD Command not recognized\r\n".encode())
    except (BrokenPipeError, ConnectionResetError, ssl.SSLError) as e:
        print(f"    [!] Connection error from {addr}: {e}")
    finally:
        conn.close()


def main():
    parser = argparse.ArgumentParser(description="Simulate an old IMAPS server with RSA-only ciphers")
    parser.add_argument("--port", type=int, default=10993)
    parser.add_argument("--cert", default="server.crt")
    parser.add_argument("--key", default="server.key")
    parser.add_argument(
        "--max-tls",
        choices=["1.0", "1.1", "1.2"],
        default="1.2",
        help="Maximum TLS version the server will accept (default: 1.2)",
    )
    args = parser.parse_args()

    ctx = build_ssl_context(args.cert, args.key, args.max_tls)

    print(f"Listening on :{args.port}")
    print(f"  Max TLS version : TLSv{args.max_tls}")
    print(f"  Cipher suites   : {RSA_ONLY_CIPHERS}")
    print(f"  Cert            : {args.cert}")
    print()
    print("Expecting JDK 21.0.10 clients to FAIL with handshake_failure")
    print("(because they no longer offer static-RSA key exchange)")
    print()

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("0.0.0.0", args.port))
        sock.listen(5)

        with ctx.wrap_socket(sock, server_side=True) as ssock:
            while True:
                try:
                    conn, addr = ssock.accept()
                    t = threading.Thread(target=handle_client, args=(conn, addr), daemon=True)
                    t.start()
                except ssl.SSLError as e:
                    print(f"[-] TLS handshake failed (this is the expected behavior): {e}")
                except KeyboardInterrupt:
                    print("\nShutting down.")
                    sys.exit(0)


if __name__ == "__main__":
    main()
