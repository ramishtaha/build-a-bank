# 🧳 Capsule - Step 8

**Exists now:**
- Modules: services/hello (:8080), playground/spring-lab, services/cif (:8081, NEW) — parent pom, Java 25, Spring Boot 4.0.6, Testcontainers 2.0.5.
- CIF endpoints: POST /api/customers (201+Location, 400 on invalid), GET /api/customers/{id} and /by-number/{customerNumber} (200/404), /actuator/flyway|health|info.
- Tests: 46 total green (+6 cif: 2 @DataJpaTest + 3 @WebMvcTest + 1 @SpringBootTest) on real Postgres via Testcontainers (PG 17.10).

**This step added:**
- services/cif module: Customer entity + KycStatus enum (PENDING/VERIFIED/REJECTED), Flyway V1__create_customer.sql, CustomerRepository (findByCustomerNumber, existsByEmail), @Transactional CustomerService, CreateCustomerRequest/CustomerResponse records + Bean Validation, CustomerController.
- application.yml: ddl-auto=validate (Flyway owns the schema), open-in-view=false, env-driven datasource (${VAR:default}), port 8081, graceful shutdown.
- ContainersConfig (@TestConfiguration + @ServiceConnection, pinned postgres:17-alpine) + compose.yaml (cif-postgres, host 5432, named volume cif-pgdata).
- steps/step-08 ships requests.http, smoke.sh, seed.sql (CIF-DEMO001..).

**Gotchas:**
- Boot 4 needs the spring-boot-flyway integration module (flyway-core alone = no auto-config); @DataJpaTest excludes Flyway → @ImportAutoConfiguration(FlywayAutoConfiguration.class) or "missing table [customer]".
- Boot 4 split test slices into per-tech modules with NEW packages (spring-boot-data-jpa-test, spring-boot-webmvc-test, spring-boot-jdbc-test); @MockBean removed → @MockitoBean.
- Testcontainers 2.0: artifacts renamed testcontainers-postgresql / testcontainers-junit-jupiter; PostgreSQLContainer non-generic (no <>), at org.testcontainers.postgresql.
- Host 5432 already taken → compose bind failure or `FATAL: password authentication failed for user "bank"` → remap 5433:5432 + SPRING_DATASOURCE_URL.
- Flyway filename needs TWO underscores (V1__create_customer.sql); applied migrations are checksummed — never edit V1, add V2.

**Callback hooks:**
- Persistence context / lazy / N+1 / @Version + the OSIV-off rationale → Step 9; idx_customer_email + EXPLAIN → Step 10; @Transactional boundaries + ledger → Step 12; ProblemDetail error bodies (400/404/409) → Step 13; database-per-service consequences (Saga/Outbox) → Step 19+.
- Real-Postgres proof convention: random high port in the Testcontainers JDBC URL (e.g. 57881), never 5432.

**Next step starts:** step-08-end == step-9-start. Green: BUILD SUCCESS, 46 tests (+6 cif), Flyway v1 migrated, live POST→201/GET→200 + 400 + 404, §12.3 mutation (404→200) caught + reverted, steps/step-08/smoke.sh PASSED.
