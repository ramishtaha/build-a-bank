# 🃏 Build-a-Bank — Cumulative Flashcards

> Spaced-repetition deck. Each step appends 3–5 Q/A pairs. Import into Anki via the optional CSV, or just self-quiz.
> Format: **Q:** question — **A:** answer.

## Step 1 — Setup, CLI/Git & first Spring Boot app
- **Q:** What three annotations does `@SpringBootApplication` combine? — **A:** `@SpringBootConfiguration`, `@EnableAutoConfiguration`, `@ComponentScan`.
- **Q:** Which Spring Boot 4 removal broke our first test, and what replaces it? — **A:** `TestRestTemplate` (+ `org.springframework.boot.test.web.client`); use `RestClient` / `RestTestClient` / `MockMvcTester`.
- **Q:** `./mvnw verify` vs `package`? — **A:** `verify` runs the full lifecycle including all tests (ends in BUILD SUCCESS only if green); `package` just builds the jar.
- **Q:** Why pin versions and never use `latest`? — **A:** reproducibility — the identical build every time; no surprise upstream breakage.
- **Q:** The chain invariant? — **A:** `step-NN-end` == `step-(NN+1)-start`, and both build clean.

## Step 2 — Java language primer
- **Q:** Why never use `double` for money? — **A:** binary floating point can't represent decimal fractions exactly → rounding errors; use `BigDecimal` with an explicit scale + `RoundingMode`.
- **Q:** What does sealing an interface buy you in a `switch`? — **A:** exhaustiveness — the compiler verifies every permitted type is handled; no `default` needed, and adding a type fails the build until you handle it.
- **Q:** What does a `record` generate for you? — **A:** the canonical constructor, accessors, `equals`, `hashCode`, `toString` — and it's shallowly immutable.
- **Q:** `Instant` vs `LocalDate` vs `ZonedDateTime` — which do you store? — **A:** store `Instant` (UTC); `LocalDate` for zone-less dates (e.g. DOB); `ZonedDateTime` only at the display edge.
- **Q:** What problem does `Optional` solve? — **A:** it models "maybe absent" in the type system so callers can't forget the not-found case → no surprise `NullPointerException`.

## Step 3 — How the Internet & the Web Work
- **Q:** What happens when you type a URL and press Enter? — **A:** DNS resolves host→IP → TCP connects (3-way handshake) to the port → TLS handshake if https → the HTTP request is sent, the response comes back and is rendered.
- **Q:** Are HTTP header *names* case-sensitive? — **A:** No — case-insensitive (RFC 9110). Never assert exact header-name case (the JDK `HttpServer` emits `Content-type`).
- **Q:** What does TLS give you? — **A:** confidentiality (encryption), integrity (tamper detection), and server authentication (via the certificate).
- **Q:** HTTP/2 vs HTTP/1.1, one key difference? — **A:** HTTP/2 is binary and multiplexes many requests over one connection (no HTTP-layer head-of-line blocking), negotiated via ALPN during the TLS handshake.
- **Q:** L4 vs L7 load balancer? — **A:** L4 routes by IP/port (transport layer); L7 routes by HTTP content (path/host/headers).

## Step 4 — How Java Runs: the JVM Up Close
- **Q:** `javac` vs `java`? — **A:** `javac` compiles `.java` → `.class` bytecode; `java` launches the JVM, loads classes, and executes bytecode (interpreting + JIT-compiling hot paths).
- **Q:** Where does class metadata live since Java 8? — **A:** **Metaspace** (native memory), which replaced the old fixed-size PermGen.
- **Q:** Default GC since Java 9? — **A:** **G1** (Garbage-First). Low-pause alternatives: ZGC, Shenandoah (tuning in Step 55).
- **Q:** What is tiered JIT compilation? — **A:** start interpreting; compile hot methods with C1 (levels 1–3) then C2 (level 4); deoptimize ("uncommon trap") back to the interpreter when assumptions break.
- **Q:** What is escape analysis? — **A:** a JIT optimization that proves an object doesn't escape its method and scalar-replaces/stack-allocates it — so no heap allocation or GC happens (we saw 5M "allocations" in 9 ms with zero GC).
