# 📖 Glossary — Build-a-Bank

A cumulative glossary: each step contributes its **Key Terms**, defined in plain words. Grouped by the step that introduces the term (a term is defined where it first matters; later steps deepen it).

> Pairs with `docs/interview-bank.md` (Q&A) and `docs/flashcards.md` (spaced repetition).

---

## Step 11 — Concurrency & Thread Safety

- **JMM (Java Memory Model)** — the JLS contract defining what one thread is guaranteed to see of another's actions. It governs three things: atomicity, visibility, and ordering.
- **Atomicity** — whether an operation happens "all at once" or can be interrupted partway by another thread. `balance += amount` is *not* atomic (it's read-modify-write).
- **Visibility** — whether a write by one thread is guaranteed to be *seen* by another. Without coordination, a write may sit in a CPU cache/register and never (or only late) become visible.
- **Ordering** — the compiler/JIT/CPU may reorder instructions for speed; another thread can observe a surprising order unless a happens-before edge forbids it.
- **happens-before** — the JMM relation that, when it holds between actions A and B, guarantees A's writes are visible to B and A is ordered before B. Created by lock unlock→lock, `volatile` write→read, `start()`, `join()`, and final-field publication.
- **race condition** — a bug whose outcome depends on the unpredictable timing/interleaving of threads.
- **lost update** — a specific race where two read-modify-write operations interleave so one update overwrites (and discards) the other.
- **`synchronized` / monitor** — Java's intrinsic lock. Entering a `synchronized` method/block acquires the object's monitor (mutual exclusion); the release→acquire pair creates a happens-before edge (visibility + ordering).
- **`volatile`** — a field modifier giving visibility + ordering (a memory barrier) and atomic single read/write, but **not** atomicity of compound actions. For flags, not counters.
- **CAS (compare-and-swap)** — a single atomic CPU instruction: "set this location to *new* only if it currently equals *expected*." The basis of lock-free algorithms; retries on conflict instead of blocking.
- **`AtomicLong` / `AtomicInteger`** — a single `volatile` value updated via a CAS loop (`addAndGet`, `incrementAndGet`). Lock-free, ideal at low/medium contention.
- **`LongAdder`** — a contention-friendly counter that stripes its value across multiple `Cell`s (threads usually hit different cells), summed on read via `sum()`. Beats `AtomicLong` under heavy write contention.
- **virtual thread** — a lightweight JVM-scheduled thread (stable Java 21) that runs many-to-few on platform carrier threads; cheap to create by the million.
- **carrier thread** — a platform (OS) thread that actually runs virtual threads; a virtual thread **mounts** a carrier to run and **unmounts** it when it blocks.
- **mount / unmount** — a virtual thread mounts onto a carrier while running and unmounts (freeing the carrier) when it blocks, which is why blocking is cheap.
- **`ExecutorService`** — a task-submission abstraction over a thread pool; `AutoCloseable` since Java 19 (its `close()` performs an orderly shutdown that *waits* for submitted tasks).
- **`CompletableFuture`** — composable asynchronous results (`thenCombine`, `thenCompose`, `allOf`) without manual thread juggling.
- **`Semaphore`** — a counting permit holder; `Semaphore(n)` lets at most `n` threads into a section at once — a clean way to bound concurrency.
- **`CyclicBarrier`** — a sync point where N threads `await()` until all arrive, then all proceed; used here to *force* a deterministic lost-update interleaving.
- **deadlock** — two+ threads each holding a lock the other needs, so none can proceed. Fixed by a global lock ordering or `tryLock` with timeout.
- **double-checked locking** — the lazy-singleton trick that is broken unless the field is `volatile` (reordering can publish a half-constructed object).
- **false sharing** — two unrelated fields on the same 64-byte cache line, causing cores to fight over the line; mitigated by `LongAdder` / `@Contended`.
- **TOCTOU (time-of-check-to-time-of-use)** — a race where state changes between checking it and acting on it (e.g. check balance, then debit); a security bug, not just a correctness one. Fix by making check-and-act atomic.

---

## Step 12 — Demand Account, the Double-Entry Ledger & Transactions

- **ACID** — the four guarantees of a database transaction: **A**tomicity (all-or-nothing), **C**onsistency (invariants hold), **I**solation (concurrent transactions don't corrupt each other), **D**urability (committed survives a crash).
- **double-entry bookkeeping** — every money movement is recorded as two entries of equal amount: a **DEBIT** on the payer and a **CREDIT** on the payee, sharing one `transactionId`. The entry log is append-only and always nets to zero, so the books always balance.
- **ledger / `LedgerEntry`** — the append-only table of entries (one leg per row). Never updated or deleted — an immutable audit trail.
- **materialized balance** — a `balance` column kept on the account and updated inside the transfer transaction, so reads are O(1); contrasted with a **derived balance** (summing the ledger on each read, O(entries)).
- **`@Transactional`** — Spring's declarative transaction boundary. A proxy opens a transaction before the method, commits on normal return, rolls back on a (runtime) exception.
- **propagation** — what happens when a transactional method calls another: `REQUIRED` (join or start), `REQUIRES_NEW` (suspend caller, run independently), `NESTED` (savepoint), plus `SUPPORTS`/`MANDATORY`/`NEVER`/`NOT_SUPPORTED`.
- **`REQUIRES_NEW`** — suspends the caller's transaction and runs in a brand-new one that commits independently; used for audit rows that must survive an outer rollback. Only takes effect across a bean boundary.
- **rollback rules** — by default `@Transactional` rolls back on `RuntimeException`/`Error` only; checked exceptions **commit** unless `rollbackFor` is set.
- **`readOnly = true`** — a transaction hint that no writes happen; Hibernate sets the flush mode to manual (skips dirty-checking) and the DB/driver can optimize.
- **self-invocation pitfall** — calling a `@Transactional` (or `@Async`/`@PreAuthorize`) method via `this.` inside the same bean bypasses the Spring proxy, so the advice never runs (Step 7).
- **optimistic locking (`@Version`)** — no lock during the read; an `@Version` column makes the `UPDATE` carry `WHERE version = ?` and `SET version = version + 1`. A stale write matches 0 rows → `ObjectOptimisticLockingFailureException`. Detect-and-retry; best for rare conflicts.
- **`ObjectOptimisticLockingFailureException`** — Spring's exception thrown when an optimistic-lock version check fails (a concurrent write bumped the version first).
- **pessimistic locking (`SELECT … FOR UPDATE`)** — take a row write-lock at read time so other transactions **block** until commit. Expressed in Spring Data as `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Lock-and-wait; best for high contention.
- **`@Lock(LockModeType.PESSIMISTIC_WRITE)`** — the Spring Data annotation that makes Hibernate emit `SELECT … FOR UPDATE` for a query method.
- **lock ordering** — acquiring multiple locks in a consistent global order (here, by account number) so concurrent operations can't form a hold-and-wait deadlock cycle (Step 11).
- **`40P01` / deadlock detection** — Postgres's error code when it detects a deadlock and aborts one transaction to break it; lock ordering avoids it entirely.
- **lost update** — two read-modify-writes interleave so one overwrites and discards the other; the failure mode the transfer lock prevents (also Step 11).
- **double-spend / TOCTOU** — spending the same balance twice via the check-then-debit race; the security framing of the lost-update bug (Step 11).
- **`BigDecimal` / minor units** — exact decimal money (`numeric(19,4)`), compared with `compareTo` (scale-insensitive), never `double`/`float`. "Minor units" = the smallest currency unit (cents) as a concept.
- **bulk `@Modifying` update** — a JPQL `UPDATE`/`DELETE` that runs directly against the table, bypassing the persistence context and `@Version` (no dirty-checking, no optimistic check) — which is why `applyBalanceUnsafe` can demonstrate a true lost update.
- **`@ServiceConnection`** — a Spring Boot 3.1+ test annotation that wires the app's `DataSource` to a running Testcontainers container automatically (no JDBC URL/credentials in test config).
- **`@Enumerated(EnumType.STRING)`** — persists an enum as its stable text name (e.g. `"DEBIT"`) rather than its ordinal, so reordering the enum can't corrupt stored rows.

---

## Step 13 — Spring MVC / REST Deep (Problem Details, OpenAPI, Filters & Interceptors)

- **`DispatcherServlet`** — the Spring MVC **front controller**: the single servlet (mapped to `/`) that receives every request and orchestrates handler mapping, argument binding, invocation, return-value handling, and exception resolution.
- **`HandlerMapping`** — the component that matches a request (path + method + headers + content type) to a handler. `RequestMappingHandlerMapping` matches `@RequestMapping`/`@GetMapping`/… and returns a `HandlerExecutionChain` (handler + interceptors).
- **`HandlerAdapter`** — the component that actually invokes the matched handler. `RequestMappingHandlerAdapter` resolves method arguments (`@PathVariable`, `@RequestParam`, `@RequestBody`, …), runs `@Valid`, calls your method, and processes the return value.
- **`HttpMessageConverter`** — converts between HTTP bodies and Java objects. The Jackson converter deserializes `@RequestBody` JSON into your record and serializes the returned DTO back to JSON.
- **content negotiation** — choosing the response representation/converter from the request's `Accept` header (and the handler's producible types). JSON by default here; errors negotiate to `application/problem+json`.
- **`@RestController`** — `@Controller` + `@ResponseBody`: a web controller whose return values are written straight to the response body (serialized by a message converter), not resolved to a view.
- **`@ControllerAdvice` / `@RestControllerAdvice`** — a global, cross-controller bean of `@ExceptionHandler` (and other) methods. `@RestControllerAdvice` adds `@ResponseBody` so handlers' return values become the response body. Spring's `ExceptionHandlerExceptionResolver` picks the most specific handler for a thrown exception.
- **`ResponseEntityExceptionHandler`** — the Spring base class for a `@ControllerAdvice` that already turns the framework's **built-in** MVC exceptions (validation, unreadable body, 404/405) into `ProblemDetail`. Extend it and override hooks (e.g. `handleMethodArgumentNotValid`) to enrich them.
- **`ProblemDetail`** — `org.springframework.http.ProblemDetail` (Spring 6+): the object representing an RFC 9457 error. Returning it from an `@ExceptionHandler` sets the HTTP status from it and serializes `application/problem+json`.
- **RFC 9457 / `application/problem+json`** — the standard "Problem Details for HTTP APIs" media type and body shape: `type` (problem-kind URI), `title`, `status`, `detail`, `instance`, plus arbitrary **extension members** (we add `errors`). Successor to RFC 7807.
- **extension member** — a custom field added to a Problem Detail beyond the five standard ones, via `problem.setProperty("errors", map)`; RFC 9457 explicitly allows them.
- **`MethodArgumentNotValidException`** — the exception thrown when `@Valid @RequestBody` binding fails Bean Validation; `ResponseEntityExceptionHandler` routes it to `handleMethodArgumentNotValid`, where we attach the per-field `errors` map.
- **422 Unprocessable Entity** — the status for a **well-formed** request that can't be fulfilled (here, an overdraw), as opposed to **400 Bad Request** for a malformed/invalid request.
- **`Filter` (`jakarta.servlet.Filter`)** — a **servlet-container-level** interceptor that wraps the entire `DispatcherServlet`; sees every request (even 404s with no handler) and has no handler context. A `Filter` bean is **auto-registered** by Boot.
- **`OncePerRequestFilter`** — Spring's base `Filter` guaranteeing the body runs **exactly once per request** (dedupes async dispatches/forwards); you implement `doFilterInternal`.
- **`HandlerInterceptor`** — a **Spring-MVC-level** interceptor that runs *around the matched handler* with three hooks: `preHandle` (before; `false` short-circuits), `postHandle` (after the handler, before commit), `afterCompletion` (always, even on exception). Must be **registered** via `WebMvcConfigurer.addInterceptors(...)`.
- **`WebMvcConfigurer`** — the callback interface for customizing Spring MVC (e.g. `addInterceptors`, CORS, formatters); all methods are `default`, so you override only what you need.
- **correlation id (`X-Request-Id`)** — a per-request identifier stamped on the response (reused from inbound or minted) so one request can be traced across logs/services; set by the `RequestIdFilter` and wired into tracing in Step 36.
- **request attribute** — per-request state stored on the `HttpServletRequest` (`setAttribute`/`getAttribute`) rather than on a shared singleton component — the thread-safe place for an interceptor's start time.
- **OpenAPI / Swagger UI** — OpenAPI is the machine-readable API spec; **Swagger UI** is a browsable HTML client rendered from it. springdoc serves the spec at `/v3/api-docs` (OpenAPI 3.1) and the UI at `/swagger-ui.html`.
- **springdoc-openapi** — the maintained library that generates the OpenAPI spec + Swagger UI from your controllers/DTOs/validation annotations. The `-webmvc-ui` starter targets the servlet stack; **3.0.x** supports Boot 4 (2.8.x targets Boot 3). Replaces the dead **springfox**.
- **`MockMvc` / `@WebMvcTest`** — a web-layer **slice** test: loads only the controller + advice + MVC infra (filters, interceptors, Jackson) with the service mocked (`@MockitoBean`); fast, no DB. Contrast with `@SpringBootTest(RANDOM_PORT)`, which boots the whole app and sends real HTTP.

## Step 14 — API Design, Versioning, Idempotency & Webhooks

- **API versioning** — letting clients pin to a compatible API shape as it evolves. Three common strategies: **URI** (`/api/v1/...` — visible, cacheable, curl-able; chosen here), **header** (`Accept: application/vnd.bank.v1+json` or `X-API-Version` — clean URLs but invisible in logs), and **media-type** (most RESTful, heaviest). New versions are added **additively**, never by mutating the old one.
- **`Deprecation` / `Sunset` / `Link` headers (RFC 8594)** — the standard, machine-readable way to retire an endpoint gracefully: `Deprecation: true` (it's deprecated), `Sunset: <HTTP-date>` (when it'll be removed), `Link: <successor-uri>; rel="successor-version"` (the replacement). The old endpoint keeps working until the sunset date — never a flag-day break.
- **idempotency** — the property that performing an operation twice has the same effect as once. `GET`/`PUT`/`DELETE` are naturally idempotent; `POST` (create a transfer) is **not** — a retry creates a second resource unless made idempotent.
- **`Idempotency-Key`** — a client-supplied, per-logical-operation key (a UUID) sent as an HTTP header so the server can dedupe retries. The server stores `key → result` and returns the stored result on any retry with the same key (Stripe's model).
- **idempotency key store** — the table (here `idempotency_key`, PK = the key) that remembers `key → transactionId`. Its **PRIMARY KEY uniqueness is the concurrency guard**: two racing duplicates can't both insert, so only one transfer commits.
- **HMAC-SHA256** — a **H**ash-based **M**essage **A**uthentication **C**ode: a keyed hash (`javax.crypto.Mac`) over the message using a shared secret. Only a secret-holder can produce a signature matching a given payload, so it proves **authenticity** (it came from us) and **integrity** (the body wasn't altered). Immune to length-extension attacks (unlike a naive `hash(secret∥body)`).
- **signed material** — exactly the bytes the signature is computed over. Here it's `"<timestamp>.<body>"` (UTF-8): binding the timestamp into the hash is what makes the replay window tamper-proof.
- **replay protection** — rejecting a captured-but-still-valid request that's re-sent later. Achieved by signing a **timestamp** into the payload and rejecting on verify any timestamp outside a **tolerance window** (e.g. ±300s), even if the signature is correct.
- **constant-time comparison** — comparing two byte arrays in time independent of where the first mismatch is (`MessageDigest.isEqual`), so an attacker can't use response timing to brute-force a signature byte by byte. Never compare signatures with `String.equals`/`==`.
- **at-least-once delivery** — a delivery guarantee where a message may arrive **one or more** times (because the sender retries on failure), but never zero. Webhook delivery here is at-least-once.
- **idempotent receiver** — a webhook consumer that dedupes repeated events (by event id) so at-least-once delivery doesn't cause double processing. The receiver's responsibility, and the reason senders can safely retry.
- **dual-write problem** — the inconsistency that arises when one operation must update two systems (here: commit the DB transaction **and** deliver the webhook) without a shared transaction. If one succeeds and the other fails, they disagree. Sending the webhook *after* commit leaves the "committed but not delivered" gap.
- **Outbox pattern** — the fix for the dual-write problem (Step 20): write the event into an `outbox` table in the **same transaction** as the business change, then a separate process delivers it and marks it sent. The event is durable iff the transaction committed.
- **`Pageable`** — Spring Data's pagination *request* object (which page, what size, what sort). Bound from `?page=&size=&sort=field,dir` by `PageableHandlerMethodArgumentResolver`; defaults supplied via `@PageableDefault`.
- **`Page<T>`** — Spring Data's pagination *result*: the slice of rows plus metadata (`totalElements`, `totalPages`, `number`, `size`). A `Page<>` repository method runs a windowed `SELECT … LIMIT/OFFSET` plus a `COUNT` query.
- **`PageResponse` (stable envelope)** — a custom DTO record (`content`, `page`, `size`, `totalElements`, `totalPages`) the API owns and serializes, **instead of** Spring Data's `Page` (whose JSON shape is an internal detail Spring warns against exposing).
- **offset pagination** — `page`/`size` (`LIMIT/OFFSET`): simple, supports random page access, but degrades at depth and can skip/duplicate rows under concurrent writes.
- **cursor (keyset) pagination** — "give me N rows after id X / createdAt T": stable and fast at any depth, but no random page access. Preferred for high-volume ledgers.
- **`@RequestHeader`** — binds an HTTP request header to a controller method parameter; `required = false` makes the header optional (the parameter is `null` when absent) — how the optional `Idempotency-Key` is read.
- **`@PageableDefault`** — sets the default page number/size/sort for an injected `Pageable` when the client omits the query params; also a cheap cap on unbounded responses.
- **Jackson 3 vs Jackson 2 (Spring Boot 4)** — Boot 4's web stack defaults to **Jackson 3** (`tools.jackson`), so a Jackson-2 (`com.fasterxml.jackson.databind`) `ObjectMapper` **bean** isn't auto-created. Injecting one fails at context startup; own a `new ObjectMapper()` or use the Jackson-3 mapper.

---

## Step 15 — API Gateway / BFF & Service-to-Service HTTP

- **API Gateway** — a single service in front of all the others that receives every external request and **routes** it to the right internal service. The natural home for **edge cross-cutting concerns** (routing, auth, rate limiting, TLS termination, correlation ids, request/response shaping). Clients see one address; internal services stay private. Business logic does **not** belong here.
- **BFF (Backend-For-Frontend)** — a gateway **specialized per client type** (a web BFF, a mobile BFF) that aggregates/tailors responses for that frontend (e.g. combine three service calls into one trimmed payload for a phone). Trade-off: more gateways to operate, each simpler and evolving with its client.
- **route** — a gateway rule: an `id`, a target `uri`, **predicates** (when it matches), and **filters** (transforms). The first matching route wins.
- **predicate** — a route's match condition (e.g. `Path=/cif/**` matches any path starting with `/cif/`; the `**` is an Ant-style wildcard for the remaining segments). Also available: method, header, host predicates.
- **filter** — a transform applied to the request and/or response of a matched route (rewrite path, add/remove headers, rate-limit, authenticate). Runs in declared order.
- **`StripPrefix=N`** — a gateway filter that removes the first **N** path segments before forwarding, so the downstream receives its own path (`/cif/api/customers/1` with `StripPrefix=1` → `/api/customers/1`).
- **`AddResponseHeader` / `AddRequestHeader`** — gateway filters that tag the response (or forwarded request) with a header — here `X-Gateway: build-a-bank` proves a filter ran; later this slot carries correlation/auth headers.
- **Spring Cloud Gateway Server WebMVC** — the **servlet** (Spring MVC) gateway (`spring-cloud-starter-gateway-server-webmvc`), as opposed to the **reactive** one (`-server-webflux`, WebFlux/Netty). We use the servlet variant to stay on the MVC + virtual-threads stack; its blocking forwards scale cheaply on virtual threads. Config prefix: **`spring.cloud.gateway.server.webmvc.routes`** (the 2025 rename; `spring.cloud.gateway.mvc.routes` is deprecated and won't bind).
- **declarative HTTP interface (`@HttpExchange`)** — an annotated **interface** describing remote calls (`@HttpExchange` base path + `@GetExchange`/`@PostExchange` methods, `@PathVariable`/`@RequestParam`/`@RequestBody` bindings). You declare *what*; Spring generates the *how*. The modern, type-safe successor to hand-written `RestTemplate` calls and to OpenFeign for in-Spring use (Spring Framework 6.1+).
- **`@GetExchange`** — the declarative-client analogue of `@GetMapping`: declares a `GET` to the given path (combined with the interface's `@HttpExchange` base path).
- **`HttpServiceProxyFactory`** — the factory that reads a `@HttpExchange` interface and builds a **dynamic proxy** implementing it, turning each method call into an HTTP request via a backing client adapter.
- **`RestClientAdapter`** — the bridge that lets `HttpServiceProxyFactory` drive its calls through a `RestClient` (`RestClientAdapter.create(restClient)`).
- **`RestClient`** — Spring Framework 6.1+'s fluent, **synchronous** HTTP client (the modern `RestTemplate` replacement). Built with a base URL and a request factory; its message converters (de)serialize bodies.
- **`JdkClientHttpRequestFactory`** — Spring's request factory wrapping the JDK's `java.net.http.HttpClient`; where you set the **read timeout** (`setReadTimeout`) for a `RestClient`.
- **connect timeout** — how long to wait to **establish** the TCP/TLS connection before giving up (set on the JDK `HttpClient`).
- **read timeout** — how long to wait for the **response** after the request is sent (set on `JdkClientHttpRequestFactory`). The one that saves you from a connected-but-slow downstream.
- **`ResourceAccessException`** — Spring's "transport-level problem" exception. A read-timeout (`HttpTimeoutException`) surfaces as this — a clean, catchable failure instead of a hung thread.
- **cascading failure** — when one slow/failed component, called without a timeout, ties up callers' threads until they're exhausted, propagating the outage system-wide. Timeouts (then circuit breakers) are the availability control against it.
- **circuit breaker** — (Step 37, Resilience4j) a resilience pattern that "opens" after a failure threshold and fails fast (or serves a fallback) without even trying, protecting both caller and callee and giving the downstream room to recover. Pairs with bulkheads and retries-with-backoff.
- **temporal coupling** — the property of synchronous calls that the caller is affected *now* if the callee is down/slow (vs async messaging, which decouples them in time).
- **`@DynamicPropertySource`** — a JUnit/Spring test hook that runs **before** the context starts, used to feed a runtime-discovered value (here a random stub-server port; in Step 8, a Testcontainers JDBC port) into Spring config before beans/routes are wired.

## Step 16 — Spring Security Deep I (Filter Chain, JWT & Password Encoding)

- **security filter chain** — the ordered list of servlet filters Spring Security places in front of your app; every request runs it *before* the `DispatcherServlet`/controller. It authenticates (else 401) then authorizes (else 403). Registered as one `FilterChainProxy` that delegates to your `SecurityFilterChain` bean(s).
- **`SecurityFilterChain` (bean)** — the modern way to configure Spring Security: a `@Bean` built from `HttpSecurity` via the lambda DSL (`http.csrf(...).sessionManagement(...).authorizeHttpRequests(...).oauth2ResourceServer(...).build()`). Replaces the **removed** `WebSecurityConfigurerAdapter` (gone in Spring Security 6).
- **authentication (401)** — establishing *who you are* from the request (here: decoding+validating the Bearer JWT). Missing/invalid identity → **401 Unauthorized** (via the `AuthenticationEntryPoint`).
- **authorization (403)** — deciding *whether the established identity is allowed* to do this (e.g. needs `ROLE_ADMIN`). Authenticated but not permitted → **403 Forbidden** (via the `AccessDeniedHandler`).
- **`authorizeHttpRequests`** — the DSL block declaring per-path access rules, evaluated **top-down, first match wins**: `permitAll()` (public), `authenticated()` (any valid identity), `hasRole("ADMIN")` (role-restricted). The successor to the older `authorizeRequests`.
- **`requestMatchers(...)`** — selects the path(s) a rule applies to (e.g. `/api/auth/login`, `/api/auth/admin`). The successor to `antMatchers` (removed in Security 6).
- **`hasRole("ADMIN")`** — requires the authority **`ROLE_ADMIN`** (Spring adds the `ROLE_` prefix for you in this check). Contrast `hasAuthority("ROLE_ADMIN")`, which expects the full string.
- **JWT (JSON Web Token)** — a compact, signed token of three base64url parts: **`header.claims.signature`**. The header names the algorithm; the claims carry identity/metadata; the signature proves integrity/authenticity. **Signed, not encrypted** — the payload is readable by anyone (don't put secrets/PII in it).
- **claims** — the JWT payload fields: `sub` (subject = username), `roles` (custom — the authorities), `iss` (issuer), `iat` (issued-at), `exp` (expiry). The decoder enforces `exp`; we map `roles` → authorities.
- **OAuth2 resource server** — a service that **validates** incoming Bearer JWTs (it doesn't issue them). Spring's `oauth2ResourceServer(jwt(...))` installs the `BearerTokenAuthenticationFilter` which decodes+validates the token and sets the `Authentication`.
- **`BearerTokenAuthenticationFilter`** — the filter that extracts `Authorization: Bearer <jwt>`, hands it to the `JwtDecoder`, and on success populates the `SecurityContext`.
- **`JwtEncoder` / `NimbusJwtEncoder`** — issues (signs) JWTs. `new NimbusJwtEncoder(new ImmutableSecret<>(secretKey))` signs with the shared HMAC secret over a `JwtClaimsSet` + `JwsHeader`.
- **`JwtDecoder` / `NimbusJwtDecoder`** — validates JWTs: `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(HS256).build()` checks the **signature** and **expiry**, with the algorithm **pinned** (so a token can't dictate `alg`).
- **`JwtAuthenticationConverter` / `JwtGrantedAuthoritiesConverter`** — maps JWT claims to Spring authorities. We set `authoritiesClaimName("roles")` and an empty `authorityPrefix` (the claim already carries `ROLE_`), so `roles:["ROLE_USER"]` → authority `ROLE_USER`.
- **HMAC (HS256)** — symmetric JWT signing: **one shared secret** signs *and* validates. Simple for one issuer+validator, but every validator can also **forge**. Needs a **≥ 256-bit (32-byte)** key or it fails at startup.
- **asymmetric signing / JWKS** — the issuer signs with a **private** key; validators use the **public** key (fetched from a **JWKS** endpoint, `/.well-known/jwks.json`) and *cannot* mint tokens. The move when many independent services validate (Step 17/41).
- **BCrypt / `BCryptPasswordEncoder`** — a deliberately **slow, salted, one-way** password hash. `encode` emits `$2a$10$<22-char-salt><31-char-hash>` (cost + salt baked in); `matches(raw, hash)` re-derives and constant-time-compares. Slow defeats brute force; per-hash salt defeats rainbow tables.
- **salt** — random bytes mixed into a password before hashing so identical passwords produce **different** hashes (no precomputed/rainbow-table attacks). BCrypt embeds a fresh 16-byte salt in each hash string.
- **cost factor (work factor)** — BCrypt's tuning knob: the hash runs `2^cost` rounds (default 10). Higher = slower = harder to brute-force; raise it as hardware improves.
- **`PasswordEncoder.matches`** — verify a raw attempt against a stored hash (constant-time). **Never** compare passwords with `equals`, and never store/log plaintext.
- **CSRF (Cross-Site Request Forgery)** — an attack that tricks a browser into sending a request with the user's **ambient** credentials (cookies/session sent automatically). Disabled here because a **stateless Bearer-token** API has no ambient credentials to ride; re-enable for cookie/session flows.
- **`SessionCreationPolicy.STATELESS`** — tells Spring Security **not** to create or use an `HttpSession`; identity is re-established from the token on every request (the stateless model that scales horizontally).
- **security headers** — safe defaults Spring Security sets on responses, e.g. `X-Content-Type-Options: nosniff` (don't MIME-sniff), `X-Frame-Options` (clickjacking), `Cache-Control`. We assert `nosniff` is present.
- **`SecurityContextHolder`** — the holder of the current `Authentication`, backed by a **`ThreadLocal`** (per request, cleared at the end). Why a controller can take an `Authentication` parameter and see only *its own* request's identity.
- **`FACTOR_BEARER`** — a Spring Security **7** *authentication-factor* authority granted alongside roles to record *how* you authenticated (here: a bearer token). An internal marker for step-up/MFA reasoning (Step 17), not a role — we filter it out of the `/me` response so the API contract reports only `ROLE_*`.
