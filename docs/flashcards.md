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

## Step 5 — Spring Core & IoC Deep
- **Q:** What is Inversion of Control? — **A:** your code declares the collaborators it needs; the container creates and injects them, instead of your code `new`-ing its own dependencies.
- **Q:** Why constructor injection over field injection? — **A:** allows `final`/immutable fields, makes dependencies explicit, fails fast if one is missing, and needs no reflection to test (just pass a mock).
- **Q:** `@Component` vs `@Bean`? — **A:** `@Component` (+ scanning) for your own classes; a `@Bean` factory method in a `@Configuration` for types you don't own or that need custom construction.
- **Q:** Singleton vs prototype scope? — **A:** singleton = one shared instance per context (the default); prototype = a new instance every time the bean is requested.
- **Q:** What is a `BeanPostProcessor`? — **A:** a container extension point invoked around every bean's initialization — how Spring itself wires AOP proxies, resolves `@Value`, etc.

## Step 6 — Spring Boot Internals & Config
- **Q:** How does Boot discover auto-configurations? — **A:** it reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Boot 2.7+/3+), each line an `@AutoConfiguration` class, then evaluates their `@Conditional*` annotations.
- **Q:** What does `@ConditionalOnMissingBean` enable? — **A:** "a sensible default you can override" — the auto-config bean is created only if you haven't defined your own.
- **Q:** `@Value` vs `@ConfigurationProperties`? — **A:** `@Value` for one property; `@ConfigurationProperties` for a typed, validated group bound by prefix (constructor binding for records).
- **Q:** How do you debug "why is/isn't this bean here?" — **A:** `GET /actuator/conditions` — the positive/negative auto-config match report (with reasons).
- **Q:** Why not expose all Actuator endpoints in prod? — **A:** `beans`/`env`/`configprops`/`conditions` leak internal structure + config; expose minimally and secure them.

## Step 7 — AOP & the Proxy Model
- **Q:** The four AOP terms? — **A:** *aspect* (the cross-cutting concern), *pointcut* (where it applies), *advice* (what runs — `@Around`/`@Before`/`@After`), *join point* (the intercepted method execution).
- **Q:** How does Spring AOP apply advice? — **A:** it wraps the bean in a runtime **proxy** (CGLIB subclass by default in Boot; JDK dynamic proxy for interface-based, `proxyTargetClass=false`); calls from outside go through the proxy.
- **Q:** The self-invocation pitfall? — **A:** a `this.method()` call inside the same bean bypasses the proxy, so `@Around`/`@Transactional`/`@PreAuthorize` advice does NOT run — a real correctness/security trap.
- **Q:** Boot 4 AOP dependency change? — **A:** `spring-boot-starter-aop` was removed; add `org.aspectj:aspectjweaver` (spring-aop is already present) and Boot auto-enables @AspectJ proxying.
- **Q:** JDK proxy vs CGLIB? — **A:** JDK dynamic proxy implements the bean's interfaces; CGLIB subclasses the class; Spring Boot defaults to CGLIB (`proxyTargetClass=true`).

## Step 8 — CIF Service (Spring Data JPA, Flyway, Testcontainers)
- **Q:** Database-per-service? — **A:** each microservice owns its own database/schema; services integrate via APIs/events, never by reaching into another service's tables.
- **Q:** Why test on real Postgres (Testcontainers) not H2? — **A:** H2 ≠ Postgres (different SQL/types/behavior); Testcontainers runs the real engine, so tests catch real issues. The random high JDBC port proves it's a live container.
- **Q:** `ddl-auto=validate` + Flyway, who owns the schema? — **A:** Flyway (versioned migrations) owns it; Hibernate only validates the entity mapping matches — it never alters the schema.
- **Q:** What is a Spring Data derived query? — **A:** a repository method whose NAME (`findByCustomerNumber`, `existsByEmail`) Spring Data parses into a query at startup — you write no implementation.
- **Q:** Why return a DTO instead of the JPA entity? — **A:** it decouples the API contract from the DB schema, avoids leaking fields/lazy associations, and dodges serialization surprises.

## Step 9 — Hibernate Performance & Correctness
- **Q:** The N+1 problem? — **A:** loading N parents and then lazily loading each one's children fires 1 + N queries; fix with a fetch join / `@EntityGraph` (a single query). Proven here: 3 statements lazy vs 1 with `@EntityGraph`.
- **Q:** What does `@EntityGraph` do? — **A:** tells Hibernate to eagerly fetch named associations for THIS query (lazy → join), without changing the entity's default fetch type.
- **Q:** `LazyInitializationException` — cause & fix? — **A:** touching a lazy association after the persistence context/transaction closed (especially with OSIV off); fix by fetching it inside the transaction (join fetch/`@EntityGraph`) or mapping to a DTO there.
- **Q:** Optimistic vs pessimistic locking? — **A:** optimistic (`@Version`) detects a conflict at commit via `WHERE version=?` — no DB locks, great for low contention; pessimistic (`SELECT … FOR UPDATE`) locks the row up front — for hot rows (Step 12).
- **Q:** Why `open-in-view: false`? — **A:** OSIV keeps the persistence context open through view rendering, hiding N+1/lazy bugs and holding DB connections longer; turning it off makes those issues fail fast and forces explicit fetching.

## Step 10 — Relational Databases Up Close
- **Q:** How do you read `EXPLAIN (ANALYZE)`? — **A:** bottom-up/inside-out; name the scan node (Seq/Index/Bitmap/Index-Only), compare **estimated vs actual rows** (big gap → stale stats → `ANALYZE`), check `Buffers` for I/O.
- **Q:** What is a covering index? — **A:** an index that `INCLUDE`s the extra columns a query reads, so it's answered from the index alone (an **Index Only Scan**, `Heap Fetches: 0`) — needs the visibility map set by VACUUM.
- **Q:** What is MVCC? — **A:** multi-version concurrency control: rows have versions (`xmin`/`xmax`); each txn reads a snapshot of committed versions, so readers and writers never block; VACUUM reclaims dead versions.
- **Q:** Which isolation level prevents which anomaly (Postgres)? — **A:** no dirty reads at any level; non-repeatable & phantom gone at REPEATABLE READ; **write skew** needs SERIALIZABLE.
- **Q:** What is write skew and how do you fix it? — **A:** two txns read overlapping data, each writes a different row, together breaking an invariant; survives REPEATABLE READ; fix with SERIALIZABLE (SSI aborts with `40001` → retry) or `SELECT … FOR UPDATE`.
- **Q:** What happens when a connection pool is exhausted? — **A:** borrowers wait up to `connectionTimeout` then throw `SQLTransientConnectionException`; `close()` returns a connection (not a socket close); size pools small, bound query time.
- **Q:** Why can't `CREATE INDEX CONCURRENTLY` run in a transaction? — **A:** it commits internally between build phases (`SQLSTATE 25001`); migration tools run it outside a transaction — a primitive of zero-downtime/expand-contract change.

## Step 11 — Concurrency & Thread Safety in Java
- **Q:** Is `i++` / `balance += x` atomic? — **A:** No — it's a read-modify-write (read, add, write); two threads can interleave and lose an update. Fix with `AtomicLong`/`LongAdder`/`synchronized`.
- **Q:** What three things is the Java Memory Model about? — **A:** atomicity, visibility, and ordering — tied together by **happens-before**.
- **Q:** What creates a happens-before edge? — **A:** monitor unlock→lock, volatile write→read, `Thread.start()`→thread actions, thread actions→`join()`, and final-field publication.
- **Q:** `volatile` vs `synchronized`? — **A:** volatile = visibility + ordering (and atomic single read/write), NOT compound atomicity; synchronized = mutual exclusion + visibility + ordering for a whole block.
- **Q:** `AtomicLong` vs `LongAdder` vs `synchronized`? — **A:** AtomicLong = lock-free CAS (low contention); LongAdder = striped cells (high contention, `sum()` aggregates); synchronized/Lock when updating multiple fields atomically.
- **Q:** What are virtual threads (Java 21)? — **A:** lightweight threads scheduled onto a few carrier OS threads; they unmount the carrier when they block, so blocking is cheap and millions are fine — but they do NOT make racy code safe or change the memory model.
- **Q:** Name three classic concurrency bugs. — **A:** deadlock (fix: lock ordering), double-checked locking (needs `volatile`), false sharing / TOCTOU (use atomic compound ops / `ConcurrentHashMap`).

## Step 12 — Demand Account, Double-Entry Ledger & Transactions
- **Q:** How do you stop two concurrent withdrawals from overdrawing an account? — **A:** make check-and-debit atomic: pessimistic `SELECT … FOR UPDATE` (lock-and-wait), or optimistic `@Version` + retry, or SERIALIZABLE + retry.
- **Q:** Optimistic vs pessimistic locking? — **A:** optimistic (`@Version`) detects a conflict at write time and retries (rare-conflict friendly); pessimistic (`FOR UPDATE`) locks at read time so others wait (high-contention friendly). We use pessimistic for hot money rows.
- **Q:** What is double-entry bookkeeping? — **A:** every movement writes two entries (DEBIT + CREDIT) of equal amount sharing a transaction id; the ledger is append-only and always nets to zero, so the books balance.
- **Q:** Does `@Transactional` roll back on a checked exception? — **A:** No — by default only on `RuntimeException`/`Error`; checked exceptions commit unless you set `rollbackFor`.
- **Q:** What does `@Transactional(propagation = REQUIRES_NEW)` do? — **A:** suspends the caller's transaction and runs in a new one that commits independently (e.g. an audit row survives an outer rollback).
- **Q:** Why does the REQUIRES_NEW audit live in a separate bean? — **A:** `@Transactional` is proxy-based; a `this.`-call (self-invocation) bypasses the proxy so the new transaction never starts (Step 7 pitfall).
- **Q:** How do you avoid deadlock when a transfer locks two rows? — **A:** acquire locks in a consistent global order (e.g. by account number) regardless of transfer direction.
- **Q:** Why `BigDecimal` for money, not `double`? — **A:** `double` is binary floating point (can't represent 0.10 exactly) and drifts; `BigDecimal` is exact decimal, stored as `numeric`, compared with `compareTo`.

## Step 13 — Spring MVC / REST Deep (Problem Details, OpenAPI, filters/interceptors)
- **Q:** Walk through a Spring MVC request. — **A:** servlet filter chain → DispatcherServlet → HandlerMapping (find the @RequestMapping) → interceptor preHandle → HandlerAdapter (bind args + @Valid + HttpMessageConverter) → handler → return value serialized to JSON; exceptions go to @ControllerAdvice.
- **Q:** What is RFC 9457? — **A:** Problem Details for HTTP APIs — a standard error body (`application/problem+json`) with type/title/status/detail/instance + custom extension members.
- **Q:** How do you return ProblemDetail for validation errors too? — **A:** make the `@RestControllerAdvice` extend `ResponseEntityExceptionHandler` and override `handleMethodArgumentNotValid`.
- **Q:** Filter vs interceptor? — **A:** filter = servlet-container level, sees every request (even 404s), no handler context (correlation ids/CORS); interceptor = Spring-MVC level, around the matched handler, has handler metadata (timing/auth). Filters auto-register; interceptors need WebMvcConfigurer.
- **Q:** What turns `@RequestBody` JSON into a Java object? — **A:** an `HttpMessageConverter` (Jackson) in the HandlerAdapter, before the controller method runs; content negotiation picks it by Accept.
- **Q:** Which springdoc version works with Spring Boot 4? — **A:** 3.0.x (e.g. 3.0.3); 2.8.x targets Boot 3. It generates OpenAPI 3.1 at /v3/api-docs and Swagger UI at /swagger-ui.html.
- **Q:** Are controllers/filters/interceptors thread-safe? — **A:** they're shared singletons, so keep them stateless; put per-request state in request attributes/ThreadLocal, never instance fields.

## Step 14 — API Design, Versioning, Idempotency & Webhooks
- **Q:** API versioning strategies? — **A:** URI (`/api/v1/…`, visible/cacheable — our choice), header (`Accept`/custom — clean URLs, invisible), media-type (most RESTful, heavy). Deprecate old paths with `Deprecation`/`Sunset`/`Link` headers (RFC 8594).
- **Q:** What makes an API idempotent, and how? — **A:** a retried request has the same effect as one request; implement with an `Idempotency-Key` header + a store (key → result). A repeat key returns the stored result without re-executing; the key's unique PK guards concurrency.
- **Q:** How do you secure outbound webhooks? — **A:** sign the payload with HMAC-SHA256 over `"<timestamp>.<body>"` (shared secret); receivers verify the signature (constant-time) AND reject stale timestamps (replay protection).
- **Q:** Webhook delivery semantics? — **A:** at-least-once (retry with backoff on failure) → receivers MUST be idempotent (they may see an event twice).
- **Q:** Why not serialize Spring Data's `Page` directly? — **A:** its JSON shape is an unstable internal detail; expose a stable DTO envelope (content + page/size/totalElements) so the API owns its contract.
- **Q:** The dual-write problem with webhooks? — **A:** the DB commits but the send fails (or vice-versa) → state and notification diverge; fixed by the Outbox pattern (persist the event in the same transaction, deliver async) — Step 20.
- **Q:** Spring Boot 4 + Jackson gotcha? — **A:** Boot 4's web stack defaults to Jackson 3, so a Jackson-2 `com.fasterxml…ObjectMapper` *bean* isn't auto-created; create your own instance or use the Jackson 3 mapper.

## Step 15 — API Gateway / BFF + service-to-service HTTP
- **Q:** What is an API Gateway / BFF? — **A:** a single front door that routes external requests to internal services and centralizes cross-cutting edge concerns (routing, auth, rate limiting, correlation ids), so clients see one endpoint instead of many.
- **Q:** Reactive vs servlet Spring Cloud Gateway? — **A:** `spring-cloud-starter-gateway-server-webflux` (WebFlux/Netty, reactive) vs `spring-cloud-starter-gateway-server-webmvc` (Spring MVC, servlet). We use the **MVC** one to stay on the MVC + virtual-threads stack; config prefix `spring.cloud.gateway.server.webmvc.routes`.
- **Q:** How do Spring services call each other today? — **A:** a declarative **HTTP interface** (`@HttpExchange`/`@GetExchange`) implemented by `HttpServiceProxyFactory` over a `RestClient` — type-safe, no hand-written plumbing; the modern successor to `RestTemplate`/OpenFeign for in-Spring calls.
- **Q:** Why set timeouts on service-to-service calls? — **A:** a call must fail fast, never hang on a slow dependency — otherwise one slow service exhausts threads/pools and cascades into a system-wide outage. Set connect + read timeouts (full resilience: circuit breakers/Resilience4j in Step 37).
- **Q:** What does a gateway `StripPrefix` filter do? — **A:** removes leading path segments before forwarding, so an external `/cif/api/customers/1` reaches the service's own `/api/customers/1`.

## Step 16 — Spring Security deep I (filter chain, JWT, BCrypt)
- **Q:** What is the Spring Security filter chain? — **A:** an ordered chain of servlet filters (a `SecurityFilterChain` bean, configured with the lambda DSL) that authenticate and authorize each request before it reaches a controller. `WebSecurityConfigurerAdapter` was removed in Security 6; use `authorizeHttpRequests`/`requestMatchers`.
- **Q:** Authentication vs authorization? — **A:** authentication = who you are (a valid identity/token → else 401); authorization = what you're allowed to do (e.g. a required role → else 403).
- **Q:** What is a JWT and how is it verified? — **A:** a signed token (header.claims.signature); the holder of the secret/key verifies the signature (HMAC-SHA256 here) and the expiry. Claims carry the subject + roles. We validate it as an OAuth2 resource server.
- **Q:** Symmetric (HMAC) vs asymmetric (RSA/EC) JWT signing? — **A:** HMAC shares one secret (any validator can also forge) — fine for one issuer+validator; asymmetric lets the issuer sign with a private key and many services validate with the public key (JWKS) without being able to mint — use it when multiple services validate.
- **Q:** Why disable CSRF for a JWT API? — **A:** CSRF attacks ride ambient cookie/session credentials; a stateless Bearer-token API has no cookie/session to ride, so CSRF protection doesn't apply (re-enable it for cookie-based browser sessions).
- **Q:** Why BCrypt for passwords? — **A:** a slow, salted, one-way hash — never store/compare plaintext; verify with constant-time `matches`. The same password hashes differently each time (per-hash salt).

## Step 17 — Spring Security deep II (resource servers, RS256/JWKS, method security)
- **Q:** HMAC vs asymmetric JWT signing — why switch to RS256 for microservices? — **A:** with HMAC every validator holds the shared secret and could forge tokens; with RS256 the issuer signs with a private key and services validate with the public key (published via JWKS) — they can verify but not mint (least privilege).
- **Q:** What is JWKS? — **A:** a JSON Web Key Set — the issuer's public signing keys at a URL (e.g. /oauth2/jwks). Resource servers fetch it (`jwk-set-uri`) to validate token signatures; supports multiple keys for rotation.
- **Q:** What makes a service an OAuth2 resource server? — **A:** it validates incoming `Bearer` JWTs (signature + expiry) on each request via a `JwtDecoder` (from `jwk-set-uri`), with no session — `oauth2ResourceServer(jwt(...))`.
- **Q:** URL authorization vs method security? — **A:** URL rules (`authorizeHttpRequests`) secure the HTTP edge coarsely; `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` express fine-grained, domain-level rules on methods (reusable across entry points).
- **Q:** What is step-up authentication? — **A:** requiring a *stronger/fresh* factor (e.g. OTP/passkey) for sensitive operations even when already logged in; Spring Security 7 models factors as `FACTOR_*` authorities you can require with `@PreAuthorize`.
- **Q:** Why is a JWT hard to revoke, and how do you cope? — **A:** it's valid until it expires (no server lookup); use short access-token lifetimes + revocable refresh tokens (and/or a denylist) — and rotate signing keys via JWKS.

## Step 18 — Secure coding & threat modeling (DevSecOps shift-left)
- **Q:** What are the six STRIDE threat categories? — **A:** Spoofing (authn), Tampering (integrity), Repudiation (non-repudiation), Information disclosure (confidentiality), Denial of service (availability), Elevation of privilege (authz). Applied *per element* over a data-flow diagram with trust boundaries.
- **Q:** What is BOLA (OWASP API1:2023) / IDOR? — **A:** Broken Object Level Authorization — an endpoint acts on an object id from the request without checking the caller owns it. It's an *authorization* bug (authentication can pass). Prevent it by deriving the owner from the authenticated principal and enforcing ownership on every access.
- **Q:** How do you actually prevent SQL injection? — **A:** parameterized queries / prepared statements (ORMs like Spring Data bind parameters) so user input is always *data*, never *code* — not escaping, not character blocklists.
- **Q:** Is CORS a security control? — **A:** No — it's a *browser* guardrail (only browsers honor the preflight); curl/other servers ignore it. Deny-by-default CORS stops a malicious website scripting your API in a victim's browser, but the JWT is the real access gate.
- **Q:** Name three secure response headers and what they do. — **A:** `X-Content-Type-Options: nosniff` (no MIME sniffing), `X-Frame-Options: DENY` (anti-clickjacking, no framing), `Referrer-Policy: no-referrer` (don't leak URLs); plus HSTS (force HTTPS, only over TLS).
- **Q:** You find a critical authz bug late — quick-fix or record it? — **A:** record it in a risk register (severity, owner, scheduled fix) and ship only a *complete* fix; a half-enforced authz fix manufactures false confidence. Real shift-left output is a prioritized risk register, not "fix everything in one PR."

## Step 19 — Distributed-systems theory & trade-offs (CAP/PACELC, consistency, quorums, clocks, delivery)
- **Q:** State the CAP theorem precisely. — **A:** During a network *partition*, a distributed store can keep either Consistency (linearizable reads) or Availability (every request answered), not both. Partitions are unavoidable, so the real choice is CP vs AP; CAP says nothing about the no-partition case. ("CA" is not a real option.)
- **Q:** What does PACELC add to CAP? — **A:** The "else" branch: if Partition → A or C; Else (normal operation) → Latency or Consistency. Even with a healthy network, syncing replicas costs round-trips — wait (consistent/slow) or ack locally (fast/maybe-stale). Stores classed like PC/EC or PA/EL.
- **Q:** Order the main consistency models. — **A:** Linearizable (one global real-time order) → Sequential → Causal (respects cause→effect) → Read-your-writes → Eventual (replicas converge once writes stop). Stronger = costlier.
- **Q:** Why does W + R > N give strong consistency? — **A:** Any read quorum (R) and write quorum (W) must share at least one replica (pigeonhole), so a read always observes the latest committed write; also W > N/2 prevents two conflicting writes both succeeding.
- **Q:** Lamport clock vs vector clock? — **A:** Lamport: one counter per process; guarantees a→b ⇒ L(a)<L(b) but the converse fails, so it can't detect concurrency. Vector: one counter per process (a vector); detects both happens-before and concurrency (a‖b), at O(N) space.
- **Q:** Is exactly-once *delivery* possible? — **A:** No — async networks give at-most-once (may lose) or at-least-once (may duplicate). Systems achieve exactly-once *effect* by tolerating at-least-once delivery and making consumers idempotent (dedupe by id) or transactional — the same idea as the Step-14 Idempotency-Key.

## Step 20 — Spring events + Kafka, the Outbox pattern & real-time push (SSE)
- **Q:** What is the dual-write problem? — **A:** You can't atomically "write the DB AND publish to a broker" — they're separate systems with no shared transaction, so a crash between the two either loses an event (write-then-publish) or invents one (publish-then-write).
- **Q:** How does the transactional Outbox solve it? — **A:** Write the event to an `outbox` table in the SAME DB transaction as the business change (atomic). A separate relay polls unpublished rows, publishes them to Kafka at-least-once, and marks them sent only after a successful send. A crash just means the relay retries — never lost.
- **Q:** `@EventListener` vs `@TransactionalEventListener`? — **A:** `@EventListener` fires immediately, even if the surrounding transaction later rolls back; `@TransactionalEventListener` fires at a transaction phase (default AFTER_COMMIT), so you only react to committed work — critical for money.
- **Q:** How do you achieve exactly-once with Kafka? — **A:** Not exactly-once *delivery* (impossible) — at-least-once delivery + an idempotent consumer (dedupe by a stable event id) or Kafka transactions/EOS. We dedupe by eventId → exactly-once *effect*.
- **Q:** SSE vs WebSocket? — **A:** SSE is one-way server→client over a long-lived HTTP response (`text/event-stream`) with auto-reconnect — simple, ideal for notifications. WebSocket is bidirectional and heavier; use it when the client must push too.
- **Q:** Boot 4 gotcha — what brings the Kafka `KafkaTemplate` bean? — **A:** `spring-boot-starter-kafka` (which pulls the `spring-boot-kafka` autoconfiguration module). Bare `spring-kafka` compiles but auto-configures nothing — same modularization lesson as Flyway needing `spring-boot-flyway` (Step 8).

## Step 21 — Payments: the Saga pattern, Redis idempotency keys & dead-letter topics
- **Q:** What is a Saga and a compensating transaction? — **A:** A long operation split into local transactions that each commit independently; if a later step fails you run *compensating* transactions to semantically undo the committed ones (a refund undoes a debit). Used when no single ACID transaction spans the steps.
- **Q:** Saga orchestration vs choreography? — **A:** Orchestration = a central coordinator drives the steps and decides compensations (easy to reason about/test). Choreography = each service reacts to events and emits the next, no coordinator (more decoupled, but the flow is implicit/harder to trace).
- **Q:** Is a Saga isolated like an ACID transaction? — **A:** No — between steps the intermediate state is visible (money has left A but not yet reached B). You design for it with pending states + idempotency + compensation; a Saga is eventually consistent, not atomic.
- **Q:** How do you make a payment idempotent with Redis? — **A:** Store Idempotency-Key → result with `SET key value NX EX ttl` (`setIfAbsent` + TTL): the first request records its paymentId; a retry with the same key returns the stored paymentId instead of charging again. Fast, shared across instances, auto-expiring.
- **Q:** What is a Dead-Letter Topic (DLQ) and why? — **A:** A topic a consumer routes messages to after retries when it still can't process them, so a poison message doesn't block the partition forever; it's quarantined for inspection/replay. In Spring Kafka: `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (→ `<topic>.DLT`).
- **Q:** Saga vs two-phase commit (2PC)? — **A:** 2PC is atomic but locks all participants until everyone votes commit — slow, fragile under failure, and unsupported by most brokers. The Saga is the modern default: available + eventually consistent, using compensation instead of global locks.

## Step 22 — Caching, async & clustered scheduling (Market Info read model)
- **Q:** How does `@Cacheable` (cache-aside) work? — **A:** On a call it computes the key and checks the cache: a HIT returns the cached value and skips the method; a MISS runs the method, stores the result, and returns it. `@CachePut` always runs and overwrites (refresh); `@CacheEvict` removes.
- **Q:** Why is a cache a CQRS read model, and what's the catch? — **A:** It's a read-optimized view separate from the authoritative write side. The catch is it's *eventually consistent* — bounded by the TTL, the refresh cadence, and (for a networked cache like Redis) write-visibility latency.
- **Q:** Your `@Scheduled` job runs on 3 instances — what happens and how do you fix it? — **A:** It runs 3× per tick (duplicate work, hammered upstreams). Guard it with a distributed lock — ShedLock's `@SchedulerLock` over a shared store (Redis/JDBC) — so only one node runs each tick; `lockAtMostFor` frees a crashed holder, `lockAtLeastFor` blocks too-fast re-runs.
- **Q:** When do virtual threads help, and how do you use them for `@Async`? — **A:** For I/O-bound, mostly-waiting, high-concurrency work — they're cheap to create and park instead of pinning an OS thread (CPU-bound work doesn't benefit). Point the `@Async` executor at virtual threads (e.g. `SimpleAsyncTaskExecutor.setVirtualThreads(true)`).
- **Q:** Why must `@Cacheable`/`@SchedulerLock`/`@Transactional` methods be called from another bean? — **A:** They're applied by a Spring AOP proxy that only intercepts *external* calls; a `this.`-call inside the same bean bypasses the proxy (the self-invocation pitfall, Step 7).

## Step 23 — Onboarding orchestration (coordinating services with compensation)
- **Q:** Orchestration vs choreography? — **A:** Orchestration = a central coordinator drives the steps and decides compensations (explicit, easy to trace; the coordinator is a coupling point). Choreography = each service reacts to events and emits the next, no coordinator (decoupled, but the flow is implicit/harder to trace).
- **Q:** How do you "undo" a multi-service operation? — **A:** Compensation — a semantic undo (e.g. a deactivate undoes a create), because each step committed independently in its own service/DB and can't be rolled back. Design the compensating action as a first-class, tested part of the flow.
- **Q:** How does identity flow across an orchestration to a secured downstream service? — **A:** Token forwarding (token relay): the orchestrator passes the caller's `Authorization` bearer on the downstream call, which the resource server validates (Step 17). A service acting on its own behalf would use a service/client-credentials token instead.
- **Q:** Why is a synchronous in-memory orchestrator not crash-safe? — **A:** It holds the flow in memory; a crash mid-flow (after step 1, before opening the account or before compensating) leaves partial state with no record of where it was. Durability needs persisted workflow/saga state + retries (or a workflow engine).
- **Q:** What does `@JsonIgnoreProperties(ignoreUnknown=true)` buy a client response record? — **A:** Tolerance — the downstream may return more fields than the client cares about; without it, Jackson fails deserialization on the unknown properties. (The annotation lives in `com.fasterxml.jackson.annotation`, available on Jackson 3 too.)

## Step 24 — Spring Batch (EOD jobs) & the Phase-D capstone
- **Q:** What is a chunk-oriented Spring Batch step? — **A:** A loop that reads an item, processes it, and once N are accumulated writes the whole chunk — all in ONE transaction (commit per chunk). Tune the chunk size for throughput vs memory/lock duration; the next chunk runs in a new transaction.
- **Q:** How do you keep one bad record from failing a whole EOD job? — **A:** Fault tolerance: `skip(Exception).skipLimit(k)` tolerates up to k bad records (recorded as skips); `retry(Transient).retryLimit(r)` re-attempts transient failures; a processor returning `null` filters an item (no work needed). The run completes; you reconcile the skips.
- **Q:** What is the JobRepository and why version its schema with Flyway? — **A:** JDBC tables (BATCH_*) recording every JobInstance/JobExecution/StepExecution — the basis for restartability. Version the schema with Flyway (and set initialize-schema=never) so it's reproducible and owned, rather than auto-re-created each startup.
- **Q:** What makes a Spring Batch job restartable? — **A:** A job is one JobInstance per its identifying JobParameters; a FAILED execution can be re-launched and resumes from the last committed chunk (the JobRepository remembers progress). A COMPLETED instance won't re-run. Design the work so a partial-then-restart doesn't double-apply (batch idempotency).
- **Q:** Exactly-once delivery vs exactly-once effect (the capstone)? — **A:** Exactly-once *delivery* across an async network is impossible. Exactly-once *effect* is what you engineer: at-least-once delivery + an Outbox (never lose the event) + an idempotent consumer (dedupe by id) — a forced duplicate is delivered but applied once.

## Step 25 — SOLID & clean code (refactoring a smelly service)
- **Q:** What are the SOLID principles? — **A:** Single Responsibility (one reason to change), Open/Closed (extend without modifying), Liskov Substitution (subtypes are substitutable), Interface Segregation (small focused interfaces), Dependency Inversion (depend on abstractions, not concretions).
- **Q:** What is refactoring (vs rewriting), and what's the safety net? — **A:** Refactoring changes a program's *structure* without changing its *behaviour*; the test suite is the safety net — green before and (unchanged) green after proves behaviour was preserved. A rewrite may change behaviour. Don't edit tests and code together.
- **Q:** What does the Dependency Inversion Principle buy you (a port + adapter)? — **A:** The consumer depends on an interface (port) it owns, not a concrete class; adapters implement it per technology. You can swap implementations without touching callers, add new ones as *extension not modification* (OCP), and mock the port to unit-test in isolation.
- **Q:** Name a few code smells and their fixes. — **A:** God method (does many things) → extract method/class (SRP); feature envy (a method reaching into another object's data) → move behaviour to the data's owner; tight coupling to a concrete type → depend on an interface (DIP); primitive obsession → introduce a type. Smells are hints, not hard rules.
- **Q:** When is introducing an abstraction over-engineering? — **A:** When there's a single stable implementation and no test-seam or second-implementation need — YAGNI. Add the abstraction when a real reason appears (a Redis adapter to add, a unit test that needs to mock it), not reflexively.

## Step 26 — Hexagonal architecture (ports & adapters) + DDD
- **Q:** What is hexagonal (ports-and-adapters) architecture and its dependency rule? — **A:** A framework-free domain at the centre, an application of use cases around it, and adapters at the edges. Source dependencies point INWARD: adapters → application ports → domain; the domain depends on nothing. Infrastructure (Kafka, HTTP, DB) is a pluggable detail, not something the core knows about.
- **Q:** Inbound (driving) vs outbound (driven) port? — **A:** Inbound = what the application OFFERS, called by driving adapters (a Kafka listener / REST controller) — e.g. `NotifyOnTransfer`. Outbound = what the application NEEDS, implemented by driven adapters (a store / a pusher) — e.g. `ProcessedEventStore`, `NotificationPublisher`.
- **Q:** Why keep the domain free of framework/transport imports? — **A:** Business rules survive framework upgrades, are unit-testable in microseconds without a container, and the dependency direction is correct (infra depends on the core, not the reverse). Parsing/messaging/persistence all live in the adapter ring.
- **Q:** When should you NOT add a DDD aggregate/repository? — **A:** When the bounded context is thin — a read/push model with no invariant-guarding consistency boundary. DDD tactical patterns (value objects, entities, aggregates, repositories, domain services) are applied where the domain warrants them, not ceremonially everywhere.
- **Q:** How do you prove a hexagonal restructure (a package move) is safe? — **A:** It's behaviour-preserving, so the integration tests' assertions don't change — only their import lines (classes moved packages). The unchanged assertions passing is the proof the structural move didn't alter behaviour.

## Step 27 — Enforcing architecture: ArchUnit + Spring Modulith (fitness functions)
- **Q:** What is an architecture fitness function? — **A:** An automated test that asserts an architectural characteristic (here, structural dependency rules) so the design can't erode silently — a violating commit fails the build, not just review. Architecture becomes executable instead of a wiki diagram that drifts.
- **Q:** Does ArchUnit analyse source or bytecode, and why does it matter? — **A:** Bytecode (it imports compiled `.class` files). An *unused* `import` is erased by the compiler, so ArchUnit can't see it — only real references (annotations, fields, parameters, calls) are dependencies. Correct semantics, but it means you must actually *use* a type to make a rule fail.
- **Q:** How do you model a hexagon's "arrows point inward" in ArchUnit? — **A:** A `layeredArchitecture()` with Domain/Application/Adapter layers: `Domain` may only be accessed by Application+Adapter, `Application` only by Adapter, `Adapter` by no one. Model Adapter as ONE layer so a documented intra-adapter coupling (e.g. web→SSE-push) is allowed while any adapter→core-inward violation still fails. Pair it with `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("org.springframework..", ...)` for framework purity.
- **Q:** How does Spring Modulith decide what a module is, and what does `verify()` check? — **A:** Each direct sub-package of the application package is a module; its types are API unless under an `internal` sub-package. `ApplicationModules.of(App.class).verify()` checks the universal modular-monolith rules: no cyclic dependencies between modules, and no access to another module's internals. It's static bytecode analysis (ArchUnit is its engine) — no Spring context, no Docker.
- **Q:** ArchUnit vs Spring Modulith — when do you reach for each? — **A:** ArchUnit when you author *bespoke* rules about a specific design (a hexagon's layer rules, naming conventions). Spring Modulith for a *derived* module model + the universal rules (cycles, internal access) + living docs (`Documenter`: C4 diagram + per-module canvas) + optional runtime module events. Use ArchUnit for custom rules, Modulith for module hygiene + docs.
- **Q:** How do you prove a fitness function actually works? — **A:** Make the architecture fail on purpose (a §12.3 mutation): annotate the pure domain with `@Component` → ArchUnit's domain-purity rule goes red; add an `event→outbox` reference where `outbox→event` already exists → Modulith reports `Cycle detected`. Revert → green. A guard you've never seen fail is worthless.
