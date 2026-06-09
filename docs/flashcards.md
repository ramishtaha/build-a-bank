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
