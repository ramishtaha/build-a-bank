# 🧳 Capsule - Step 3

**Exists now:** parent POM (Java 25 / Spring Boot 4.0.6) · `services/hello` (Step 1; `make run-hello` → :8080 `/api/hello`) · `playground/java-basics` (Steps 2–3) with new `com.buildabank.basics.net` package. `./mvnw -pl playground/java-basics -am verify` → BUILD SUCCESS, **20 tests** (+4 net), 0 failures. Verification tier: 🟠 Standard.

**This step added:**
- `UrlAnatomy` (record) — URL → scheme/host/port/path/query; scheme-default ports 80/443; empty path → `/`.
- `HttpClientDemo` — `java.net.http.HttpClient` GET with 5s connect timeout; prints negotiated version/status/content-type/body.
- `RawHttpDemo` — raw `Socket`; hand-typed HTTP/1.1 request (CRLF lines, blank line, `Connection: close`).
- `UrlAnatomyTest` (2 tests) + `LoopbackHttpTest` (2 tests; JDK `HttpServer` on ephemeral port 0) — the +4 net tests.
- `steps/step-03/smoke.sh` — runs verify + asserts the two demo `.class` files exist; PASSED.

**Gotchas:**
- HTTP header names are case-insensitive (RFC 9110); JDK `HttpServer` emits `Content-type` → assert with `containsIgnoringCase`, never exact case (the step's marquee bug).
- `RawHttpDemo` reads to end-of-stream: it needs `Connection: close` AND the final blank `\r\n`, or the call hangs.
- On Windows, curl uses the schannel TLS backend — less handshake detail than OpenSSL builds (platform-honesty note in the Verification Log).

**Callback hooks:**
- Ephemeral-port trick (bind port 0, read the real port back) returns with Testcontainers in Step 8.
- `HttpClient` is what Spring `RestClient` wraps (Step 15); the L7-LB concept becomes the API Gateway (Step 15), full LB depth in Step 51.
- TLS guarantees + "never trust the network" extend into Steps 41/43 (TLS depth, mTLS, zero-trust).

**Next step starts:** tag `step-03-end` == `step-04-start`; green: `./mvnw -pl playground/java-basics -am verify` (20 tests, BUILD SUCCESS) + `smoke.sh` PASSED. Step 4 = How Java Runs (the JVM).
