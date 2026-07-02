# PROJECT-MAP — AI reference for accurate step callbacks

Purpose: let a lesson-generating AI session make correct callbacks ("remember Step 20?") without re-reading old lessons. Facts checked against repo on 2026-07-02.

## Modules

| Module | Path | Port | Infra | Introduced | Key patterns (steps) |
|---|---|---|---|---|---|
| hello | `services/hello` | 8080 | — | Step 1 | first Boot app + actuator (1); autoconfig conditions demo (6); consumes libs/common starter + Boot-4 MockMvcTester (28) |
| cif | `services/cif` | 8081 | Postgres | Step 8 | JPA + Flyway + Testcontainers (8); N+1 / @EntityGraph / @Version (9); raw-JDBC DB labs — plans, isolation, write skew, pooling (10); `POST /api/customers/{id}/deactivate` compensation target (23) |
| demand-account | `services/demand-account` | 8082 | Postgres, Kafka, Redis | Step 12 | double-entry ledger + pessimistic/optimistic locking (12); ProblemDetail RFC 9457 + OpenAPI + filters/interceptors (13); `/api/v1` + idempotency + signed webhooks + pagination (14); OAuth2 resource server via JWKS (17); headers + deny-by-default CORS hardening (18); @TransactionalEventListener + Outbox→Kafka relay, topic `transfers.completed` (20); Payment Saga + compensation + Redis Idempotency-Key (21); Spring Batch EOD interest accrual, Flyway V4 (24); Spring Modulith 9 cycle-free modules (27) |
| auth | `services/auth` | 8083 | — (in-memory users) | Step 16 | SecurityFilterChain + HS256 JWT + BCrypt (16); RS256 + public `/oauth2/jwks` + @PreAuthorize (17); rotating refresh-token store (reuse detection + 3s race grace), `/api/auth/refresh` (200/409/401) + `/logout`, httpOnly `bab_refresh` cookie, access TTL 10 min (32) |
| notification | `services/notification` | 8084 | Kafka (no DB) | Step 20 | idempotent @KafkaListener (exactly-once effect) + SSE stream (20); DLQ → `transfers.completed.DLT` (21); SOLID refactor, ProcessedEventStore port (25); hexagonal domain/application/adapter (26); ArchUnit hexagon rules (27); PITest 100% mutation on core (28) |
| market-info | `services/market-info` | 8085 | Redis (no DB) | Step 22 | @Cacheable Redis read model, @Async on virtual threads, @Scheduled + ShedLock (22) |
| onboarding | `services/onboarding` | 8086 | — (no DB) | Step 23 | orchestration across cif + demand-account via @HttpExchange clients, token forwarding, compensation (23) |
| gateway | `gateway` | 8080 | — | Step 15 | Spring Cloud Gateway Server WebMVC (servlet), StripPrefix routes + @HttpExchange CifClient (15); fronts auth + deny-by-default CORS for SPA (29); fronts notification SSE (30); serves the SPA via `Path=/**` catch-all (LAST route — order is list position, regression-tested) + credentialed CORS + wildcard-origin startup guard (32) |
| libs/common | `libs/common` | — | — | Step 28 | `common-spring-boot-starter`: @AutoConfiguration + AutoConfiguration.imports + @ConditionalOnMissingBean MoneyFormatter (28) |
| frontend (npm, not Maven) | `frontend/` | 5173 (Vite dev) · 5175 (nginx container) | — | Step 29 | React 19 + TS + Vite; typed API client → gateway, AuthContext, ProtectedRoute (29); TanStack Query + RHF/Zod transfer form + SSE EventSource (30); MSW/axe/i18n/Playwright gates (31); in-memory tokenStore + silent refresh + 401-retry, lazy routes + react-vendor chunk, nginx Dockerfile, full-stack capstone `e2e-fullstack/` (32) |
| java-basics | `playground/java-basics` | — | — | Step 2 | modern Java: records/sealed/streams; net + JVM labs (2–4) |
| spring-lab | `playground/spring-lab` | — | — | Step 5 | DI/scopes/SpEL/lifecycle (5); @ConfigurationProperties + custom autoconfig (6); AOP @Around + proxy self-invocation pitfall (7) |
| concurrency-lab | `playground/concurrency-lab` | — | — | Step 11 | deterministic lost update (CyclicBarrier), atomics/LongAdder, virtual threads, Semaphore (11) |
| distributed-lab | `playground/distributed-lab` | — | — | Step 19 | Lamport/vector clocks, quorum W+R>N, delivery semantics, CAP/PACELC — pure JVM (19) |

## Conventions

- Tag chain: `step-NN-start == step-(NN-1)-end`; every verified step tagged `step-NN-end`. Latest verified tag: `step-33-end` (== `step-34-start`). Phase F complete; Phase G in progress.
- **Containers (Step 33):** ONE `deploy/Dockerfile.service` (ARG MODULE/PORT; temurin-25-jdk-alpine build + locked `.m2` cache mount → jarmode layered extract → `gcr.io/distroless/java25-debian13:nonroot`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`) builds all 7 service images `bab-<svc>:0.1.0-SNAPSHOT`. `deploy/compose.fullstack.yaml --profile bank` = 11 containers (whole bank, zero host services; `make bank-up`); default profile = Step-32 hybrid (infra+SPA). Postgres initdb adds DB `cif`; Redpanda dual listeners (containers `redpanda:29092`, host `localhost:9092`). Gateway rewired by env only (`SERVICES_*_URI`). Jib incompatible with JDK 25 (VERSIONS); Buildpacks works, benched (ADR-0024).
- Package root: `com.buildabank.*` (Maven groupId `com.buildabank`; parent = Spring Boot 4.0.6 starter parent, Java 25, Spring Cloud 2025.1.1).
- Tests use real infra via Testcontainers `@ServiceConnection` (Postgres per service, Redpanda for Kafka, `redis:7.4-alpine`); prod config is env-driven with localhost defaults.
- Ports source of truth = each module's `src/main/resources/application.yml`. Gateway and hello both default to 8080 (not run together).
- Quality gates on every module since Step 28: Spotless + Checkstyle bound to `verify`; PITest via `-Pmutation`; Error Prone/NullAway via off-by-default `-Perrorprone`.
- Money = BigDecimal; time = UTC/Instant. Versions pinned in `VERSIONS.md`, never `latest`. Resume state lives in `PROGRESS.md`.

## ADR index (`adr/`)

- 0001 record-architecture-decisions · 0002 jdk-25-and-spring-boot-4 · 0003 phase-a-learning-modules · 0004 db-concept-labs-raw-jdbc · 0005 ledger-design-and-locking · 0006 api-versioning-and-idempotency · 0007 api-gateway-and-service-to-service
- 0008 auth-service-and-jwt-security · 0009 asymmetric-jwt-resource-servers · 0010 threat-model-and-secure-defaults · 0011 events-outbox-kafka · 0012 payment-saga-redis-idempotency-dlq · 0013 caching-async-shedlock-market-info · 0014 onboarding-orchestration
- 0015 spring-batch-eod-and-phase-d-capstone · 0016 solid-refactor-notification-ports · 0017 hexagonal-notification-service · 0018 archunit-and-spring-modulith · 0019 testing-quality-mastery-and-custom-starter · 0020 frontend-foundations-and-gateway-front-door · 0021 frontend-data-forms-live-updates · 0022 frontend-testing-accessibility-i18n · 0023 frontend-session-hardening-and-spa-shipping

## Gateway routes (gateway `application.yml`, port 8080)

| Path prefix | Target | Filter |
|---|---|---|
| `/cif/**` | cif :8081 | StripPrefix=1 |
| `/bank/**` | demand-account :8082 | StripPrefix=1 |
| `/api/auth/**` | auth :8083 | no strip (auth paths already `/api/auth/*`) |
| `/notifications/**` | notification :8084 | StripPrefix=1 (SSE streams fine — `text/event-stream` is in default streaming-media-types; exact-match footgun: a charset param would re-buffer) |
| `/**` (LAST — order is law) | SPA nginx container :5175 | no strip; unknown paths → index.html 200 (32) |

market-info (:8085) and onboarding (:8086) are NOT fronted by the gateway as of step-30-end. CORS: deny-by-default `GatewayCorsConfig`, default allowed origin `http://localhost:5173` (`APP_CORS_ALLOWED_ORIGINS`). All routes add `X-Gateway: build-a-bank`.

Frontend SPA talks ONLY to the gateway (single origin): auth login via `/api/auth`, account/transfers via `/bank`, SSE via `/notifications`. Carry-forward security debt: R-001 BOLA, R-002 no app-auth on cif/notification/market-info/onboarding, JWT-in-localStorage hardening due Step 32 (see `security/risk-register.md`).
