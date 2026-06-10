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

1. **Spring AI** — only **2.0.0-RC1** exists on the **Boot-4** line today (1.1.7 GA targets the Boot-3 line).
   Phase I (Step 46+) is months of learner-effort away. **Action:** re-pin to Spring AI **2.0.0 GA** when Phase I is
   reached; if still RC, document the step-back and pin the newest RC, or run the Python FastAPI sidecar path instead.
2. **ErrorProne / NullAway** — historically lag new JDKs and may not yet support **JDK 25** bytecode.
   **Action:** at Step 28 (code-quality), verify support; if absent, keep **Spotless + Checkstyle** (which work on 25)
   and document ErrorProne/NullAway as "enable when JDK-25 support ships." Never block the build on an unsupported tool.
3a. **Spring Boot 4 modularization (Step 8 findings):** test slices moved to per-tech modules — `@DataJpaTest`∈`spring-boot-data-jpa-test`, `@WebMvcTest`/`@AutoConfigureMockMvc`∈`spring-boot-webmvc-test`, `@AutoConfigureTestDatabase`∈`spring-boot-jdbc-test`; **Flyway** needs the Boot integration module `spring-boot-flyway` (the `flyway-core` library alone gives no `FlywayAutoConfiguration`), and `@DataJpaTest` excludes Flyway (use `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`); `@MockBean`→`@MockitoBean`.
3. **`TestRestTemplate` REMOVED in Spring Boot 4** (along with `org.springframework.boot.test.web.client`).
   Replacements: **`RestTestClient`** and **`MockMvcTester`** (Spring Framework 7, `org.springframework.test.web.servlet.client`).
   We hit this for real in Step 1 — it's now a "Then vs Now" teaching moment. The course uses `RestClient` / `RestTestClient`.

## Infra image tags (pinned when introduced — never `latest`)
- Postgres, Redis, Redpanda, Prometheus/Grafana/Loki/Tempo image **digests** are pinned in the step that adds them
  (Steps 8, 20, 22, 36). Recorded here as they land.

## Reproducibility
`./mvnw verify` twice yields the same result. Prerequisites + cross-platform notes live in the README.
