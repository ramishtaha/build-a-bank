# 📌 VERSIONS.md — the pinned, mutually-compatible version set

> **Resolved & pinned:** 2026-06-09 · **Rule:** never `latest`; prefer the newest *stable* the ecosystem supports.
> **Status: ✅ VERIFIED TO BUILD TOGETHER** — `./mvnw verify` is green on this set (proof: `steps/step-01/lesson.md`
> §🔬 Verification Log, and `services/hello`). See [adr/0002-jdk-25-and-spring-boot-4.md](adr/0002-jdk-25-and-spring-boot-4.md).

## Core toolchain (pinned)

| Thing | Pinned version | Where pinned |
|---|---|---|
| **Java (JDK)** | **25.0.3 LTS** (Oracle; Temurin 25 equivalent fine) | `pom.xml` `<java.version>25</java.version>`, `.tool-versions` |
| **Maven** | **3.9.12** | `./mvnw` (`.mvn/wrapper/maven-wrapper.properties`) |
| **Spring Boot** | **4.0.6** (GA — latest stable; 4.1.0 is still RC) | parent `pom.xml` |
| **Spring Framework** | **7.0.x** (managed by Boot 4.0.6) | transitive |
| **Spring Cloud** | **2025.1.1** (GA — the Boot-4.0 release train) | parent `dependencyManagement` |
| **Node.js / npm** | **22.20.0 / 11.16.0** | `frontend/` (added Phase F) |
| **Python** | **3.13.7** | `ml/` venv (added Phase I) |

## Managed library BOMs (verified GA on the Boot-4 line)

| Library | Pinned | First used | Verified GA? |
|---|---|---|---|
| Testcontainers | 2.0.5 (Boot-managed; modules renamed `testcontainers-*` in 2.0; `PostgreSQLContainer` non-generic) | Step 8 | ✅ |
| Flyway | **11.14.1** (Boot-managed; needs `spring-boot-flyway` integration module + `flyway-database-postgresql`) | Step 8 | ✅ |
| Hibernate ORM / Validator | 7.2.12.Final / 9.0.1.Final (Boot-managed) | Step 8 | ✅ |
| PostgreSQL JDBC driver | 42.7.10 (Boot-managed) | Step 8 | ✅ |
| **Postgres image** | `postgres:17-alpine` (digest `sha256:979c4379dd698aba0b890599a6104e082035f98ef31d9b9291ec22f2b13059ca`; reports PostgreSQL 17.10) | Step 8 | ✅ |
| ArchUnit (junit5) | 1.4.2 | Step 27 | ✅ |
| Spring Modulith | 2.0.6 | Step 27 | ✅ (2.x = Boot 4 line) |
| **PITest** | **1.25.4** (+ `pitest-junit5-plugin` **1.2.2**) | Step 28 | ✅ (mutation testing. **NOT 1.19.1** — its ASM fails on JDK-25 bytecode `major version 69`; 1.25.4 reads it. Maven Central search API was stale → verified latest via GitHub releases. `-Pmutation` profile; 100% on the notification core — `steps/step-28`) |
| **jqwik** | **1.9.3** | Step 28 | ✅ (property-based testing; own JUnit-Platform engine; 1000 generated cases — `steps/step-28`) |
| **Spotless (maven plugin)** | **3.6.0** | Step 28 | ✅ (lean: removeUnusedImports + whitespace + EOF newline; **`lineEndings=PRESERVE`** to avoid CRLF→LF churn. Bound to `verify`) |
| **Checkstyle / maven-checkstyle-plugin** | **13.5.0 / 3.6.0** | Step 28 | ✅ (lean ruleset `config/checkstyle/checkstyle.xml`; 0 violations repo-wide; `violationSeverity=warning` fails the build. Bound to `verify`) |
| **Error Prone / NullAway** | **2.49.0 / 0.13.6** | Step 28 | ✅ **verified working on JDK 25** (compiled `libs/common`; a planted `@Nullable` deref was flagged). Off-by-default `-Perrorprone` profile at `:WARN` (needs javac `--add-exports/--add-opens`). NOT the historical "lags JDK" outcome. |
| Resilience4j | 2.4.0 | Step 37 | ✅ (verify Boot-4 artifact at that step) |
| **springdoc-openapi** | **3.0.3** (NOT Boot-managed → pinned explicitly in `services/demand-account/pom.xml`) | Step 13 | ✅ (3.0.x supports Boot 4 / Spring 7; 2.8.x targets Boot 3. Verified: resolves + boots, live `/v3/api-docs` returns OpenAPI 3.1 — `steps/step-13` Verification Log) |
| **Spring Cloud Gateway (server-webmvc)** | `spring-cloud-starter-gateway-server-webmvc` (BOM-managed by Spring Cloud 2025.1.1 → 5.0.x). The **SERVLET/MVC** variant, NOT reactive (ADR-0007). | Step 15 | ✅ (resolves on the BOM; gateway boots and routes to a stub with `StripPrefix`/`AddResponseHeader` — `steps/step-15` / `gateway/GatewayRoutingTest`. Config prefix: `spring.cloud.gateway.server.webmvc.routes`; `…gateway.mvc.routes` deprecated.) |
| **Spring Kafka** | `org.springframework.kafka:spring-kafka` (+ `spring-kafka-test`) — Boot-managed | Step 20 | ✅ (producer in demand-account's Outbox relay + `@KafkaListener` consumer in notification; verified end-to-end on Redpanda via Testcontainers — `steps/step-20`) |
| **Redpanda image** | `redpandadata/redpanda:v24.2.7` (digest `sha256:82a69763bef8d8b55ea5a520fa1b38f993908ef68946819ca1aed43541824c48`; Kafka-API-compatible broker) | Step 20 | ✅ (Testcontainers `RedpandaContainer` + Spring Boot `@ServiceConnection`; relay publishes & consumer receives — `steps/step-20` Verification Log) |
| **Testcontainers Redpanda** | `org.testcontainers:testcontainers-redpanda` (BOM 2.0.5) | Step 20 | ✅ |
| **Spring Data Redis** | `org.springframework.boot:spring-boot-starter-data-redis` (Lettuce, Boot-managed) | Step 21 | ✅ (Redis-backed Idempotency Key for the payment Saga; verified on Testcontainers Redis — `steps/step-21`) |
| **Redis image** | `redis:7.4-alpine` (digest `sha256:6ab0b6e7381779332f97b8ca76193e45b0756f38d4c0dcda72dbb3c32061ab99`) | Step 21 | ✅ (Testcontainers `GenericContainer` + Spring Boot `@ServiceConnection(name="redis")`) |
| **ShedLock** | `net.javacrumbs.shedlock:shedlock-spring` + `shedlock-provider-redis-spring` **6.10.0** (NOT Boot-managed → pinned explicitly in `services/market-info/pom.xml`) | Step 22 | ✅ (clustered `@Scheduled` lock via Redis; verified — lock held blocks a second acquire, releasable. `steps/step-22`) |
| **Spring Cache (Redis)** | `spring-boot-starter-cache` + `spring-boot-starter-data-redis`, `spring.cache.type=redis` (Boot-managed) | Step 22 | ✅ (`@Cacheable` FX-rate read model on Redis; cache hit proven — provider called once for two reads) |

## ⚠️ Flagged step-backs / watch-items (honesty per the compatibility caveat)

1. **Spring AI** — as of the 2026-06-09 pin, only **2.0.0-RC1** exists on the **Boot-4** line (1.1.7 GA targets the Boot-3 line).
   Phase I (Step 46+) is months of learner-effort away. **Action:** re-pin to Spring AI **2.0.0 GA** when Phase I is
   reached; if still RC, document the step-back and pin the newest RC, or run the Python FastAPI sidecar path instead.
2. **ErrorProne / NullAway** — ✅ **RESOLVED at Step 28**: verified working on **JDK 25** (2.49.0 / 0.13.6 — see the
   Error Prone / NullAway row above; the historical "lags new JDKs" outcome did NOT occur). Kept as the off-by-default
   `-Perrorprone` profile at `:WARN` (needs javac `--add-exports/--add-opens`); Spotless + Checkstyle remain the
   always-on gates bound to `verify`.
3a. **Spring Boot 4 modularization (Step 8 findings):** test slices moved to per-tech modules — `@DataJpaTest`∈`spring-boot-data-jpa-test`, `@WebMvcTest`/`@AutoConfigureMockMvc`∈`spring-boot-webmvc-test`, `@AutoConfigureTestDatabase`∈`spring-boot-jdbc-test`; **Flyway** needs the Boot integration module `spring-boot-flyway` (the `flyway-core` library alone gives no `FlywayAutoConfiguration`), and `@DataJpaTest` excludes Flyway (use `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`); `@MockBean`→`@MockitoBean`.
3. **`TestRestTemplate` gone from the default test classpath in Spring Boot 4** — precise status (verified
   at Step 32 against the Boot 4.0 migration guide): the old package `org.springframework.boot.test.web.client`
   is removed; the class itself *moved* to the separate `spring-boot-resttestclient` artifact and is no longer
   auto-configured (needs `@AutoConfigureTestRestTemplate` + an explicit dependency). Practically removed for
   us; the course uses **`RestTestClient`** / **`MockMvcTester`** (Spring Framework 7) — hit for real in Step 1.

4. **Jib** — **verified INCOMPATIBLE with JDK 25 at Step 33**: `jib-maven-plugin` **3.4.5** (newest on Maven
   Central at pin date, checked via the search API) fails with `Unsupported class file major version 69` —
   the same bundled-ASM-lags-the-JDK failure as PITest 1.19.1 (row above). The course containerizes via the
   hand-written `deploy/Dockerfile.service` (ADR-0024); Buildpacks (`spring-boot:build-image`) verified
   WORKING on Java 25 (Paketo/Liberica). Re-probe Jib when a release >3.4.5 appears.

## Infra image tags (pinned when introduced — never `latest`)
- Postgres, Redis, Redpanda, Prometheus/Grafana/Loki/Tempo image **digests** are pinned in the step that adds them
  (Steps 8, 20, 22, 36). Recorded here as they land.
- **SPA container base images (Step 32,** `frontend/Dockerfile`**):**
  `node:22.20.0-alpine` (digest `sha256:dbcedd8aeab47fbc0f4dd4bffa55b7c3c729a707875968d467aaaea42d6225af`; build stage — matches the Node pin above) ·
  `nginx:1.28.3-alpine` (digest `sha256:a8b39bd9cf0f83869a2162827a0caf6137ddf759d50a171451b335cecc87d236`; reports nginx/1.28.3; serve stage).
- **Java service base images (Step 33,** `deploy/Dockerfile.service`**):**
  `eclipse-temurin:25-jdk-alpine` (digest `sha256:5ecfde8e5ecde5954ea3721155b345ef56c1d579b940c761318ad4c05959a151`; build + extract stages) ·
  `eclipse-temurin:25-jre-alpine` (digest `sha256:28db6fdf60e38945e43d840c0333aeaec66c15943070104f7586fd3c9d1665b0`; lesson comparison stage only) ·
  `gcr.io/distroless/java25-debian13:nonroot` (digest `sha256:dade01b669efd3bea3977f73cc196c56f1ee678a71ec8305f84ec15fd5a23c8d`; runtime — uid 65532, Temurin-25 JRE inside per `docker history`).
  Buildpacks builder (taught, benched): Boot's default `paketobuildpacks/builder-noble-java-tiny:latest` — note the `:latest`; pin via `spring-boot.build-image.builder` if ever adopted (ADR-0024).

## Frontend (npm) — pinned by `frontend/package-lock.json` (Step 29, Phase F)
The SPA is a separate Node/npm project (not a Maven module). `package.json` carries ranges; the **committed
`package-lock.json` is the real pin** — `npm ci` reproduces the exact tree. Resolved at Step 29 (Node 22.20.0 / npm 11.16.0):

| Package | Resolved | Notes |
|---|---|---|
| react / react-dom | 19.2.7 | function components + hooks |
| react-router-dom | 7.17.0 | client-side routing + guard |
| vite | 6.4.3 | dev server (native ESM) + prod build (Rollup/esbuild) |
| @vitejs/plugin-react | 4.7.0 | React fast-refresh |
| typescript | 5.9.3 | strict mode |
| vitest | 3.2.6 | Vite-native test runner (jsdom) |
| @testing-library/react | 16.3.2 | component/route tests |
| jsdom | 25.0.1 | DOM for tests (note: localStorage shim in src/test/setup.ts) |
| eslint / typescript-eslint | 9.39.4 / 8.61.0 | the SPA's quality gate (flat config) |
| @tanstack/react-query | 5.101.0 | server state — cache, loading/error, invalidation (Step 30) |
| react-hook-form | 7.78.0 | form state + submission (Step 30) |
| zod / @hookform/resolvers | 3.25.76 / 3.10.0 | schema validation + the RHF resolver (Step 30; zod 3 line) |
| msw | 2.14.6 | network-level API mocking for the Vitest suite (Step 31) |
| i18next / react-i18next | 24.2.3 / 15.7.4 | i18n with synchronous bundled resources (Step 31) |
| @playwright/test | 1.60.0 | E2E in real Chromium; browsers verified installed here (Step 31) |
| axe-core | 4.12.0 | accessibility assertions (Step 31) |
| rollup-plugin-visualizer | 7.0.1 | bundle treemap (`dist/stats.html`) for the Step-32 code-splitting work |

## Reproducibility
`./mvnw verify` twice yields the same result; `npm ci` in `frontend/` reproduces the locked SPA tree.
Prerequisites + cross-platform notes live in the README.
