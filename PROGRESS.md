# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, `git checkout` the last verified tag, re-runs `make doctor`, then continues
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** B — Data, Databases, Concurrency & Transactions 🔵 (in progress; Steps 8–12)
- **Step:** 11 of 67 — *Concurrency & Thread Safety in Java* — ✅ **COMPLETE & VERIFIED** (🔴 Full tier)
- **Last verified tag:** `step-11-end` (== `step-12-start`) — new module `playground/concurrency-lab` (pure JVM, no Docker), **8 tests**, full-repo `./mvnw verify` → BUILD SUCCESS (all 6 modules). Lost-update race proven **deterministically** (CyclicBarrier → balance=1 not 2) and at scale (`UnsafeBalance` 8×100k lost ~609k of 800k); fixes exact at 800k via `synchronized`/`AtomicLong`/`LongAdder`. Toolkit: virtual threads (10k tasks), `CompletableFuture`, `Semaphore(3)` capped at 3. §12.3 mutation (remove `synchronized` → `expected 800000 but was 145225`) caught + reverted; smoke.sh PASSED. JCStress taught **verify-adjacent** (§12.8 — long specialist harness, JDK-25 caveat). concurrency-lab follows the `playground/*` convention (ADR-0003).
- **Next action:** **Step 12 — Demand Account + double-entry ledger + transaction management deep** 🔵 (Phase B capstone-adjacent, milestone): a NEW service `services/demand-account` with accounts + a double-entry ledger (money in **BigDecimal** minor units, time UTC/`Instant`); Spring **`@Transactional`** deep (propagation REQUIRED/REQUIRES_NEW/NESTED, isolation, rollback rules, readOnly), **pessimistic locking** (`SELECT … FOR UPDATE` / `@Lock(PESSIMISTIC_WRITE)`) vs Step-9 optimistic `@Version`; correct under **concurrent transfers** (combine Step 10 isolation + Step 11 thread-safety); **expand-contract migration intro** (Flyway). 🎓 **Phase B Capstone:** a concurrency stress test against the ledger that **fails without locking and passes with it** — show it both ways with pasted output; justify the isolation level. Likely 🔴 Full tier (money + concurrency + new service). Docker required (Testcontainers Postgres). Keep `step-12-end == step-13-start`.

## Done so far
- ✅ **Step 0 — capability preflight** → `CAPABILITIES.md` (JDK 25.0.3 LTS, Maven 3.9.12, Docker running, no local k8s, scanners install-on-demand).
- ✅ **Version set resolved & verified to build** → `VERSIONS.md` + `adr/0002` (Java 25 + Spring Boot 4.0.6 + Spring Cloud 2025.1.1; Spring AI flagged for re-pin at Phase I).
- ✅ **Repo scaffold** — parent POM, Maven Wrapper, Makefile, `.gitignore`/`.env.example`/`.editorconfig`/`.tool-versions`, ADR-0001/0002, README, COURSE.md.
- ✅ **Step 1** — `steps/step-01/lesson.md` (full §8 contract), `requests.http`, `smoke.sh`, and the working `services/hello` app. Tagged `step-01-start` (scaffold) and `step-01-end` (verified green).
- ✅ **Steps 2–11** — all tagged `step-NN-end` and green (see the Verification ledger below). Phase A complete (Steps 1–7: language, web, JVM, Spring Core/Boot/AOP). Phase B in progress: Step 8 (CIF + JPA + Flyway + Testcontainers), Step 9 (Hibernate perf/correctness), Step 10 (relational DB up close — query plans, MVCC/isolation, write skew, pool, partitioning, online DDL), Step 11 (concurrency & thread safety — JMM, lost-update race + fixes, virtual threads). Modules so far: `services/hello`, `services/cif`, `playground/{java-basics, spring-lab, concurrency-lab}` per ADR-0003.

## Verification ledger (most recent first)
| Tag | Tier | `./mvnw verify` | Proof |
|---|---|---|---|
| `step-11-end` | 🔴 Full | BUILD SUCCESS · full repo (6 modules); concurrency-lab **8** tests | New `playground/concurrency-lab` (pure JVM). Lost update **deterministic** (CyclicBarrier → balance=1 not 2); `UnsafeBalance` 8×100k lost ~609k of 800k; `synchronized`/`AtomicLong`/`LongAdder` exact at 800k. Virtual threads 10k/10k; `Semaphore(3)` capped at 3. §12.3 mutation (remove `synchronized`) → `expected 800000 but was 145225`, reverted; `steps/step-11/smoke.sh` PASSED. JCStress verify-adjacent (§12.8). |
| `step-10-end` | 🔴 Full | BUILD SUCCESS · cif **21** tests (+11 raw-JDBC labs) | Real Postgres 17.10 via Testcontainers 2.0.5 (random port e.g. 49575). Plans: Seq Scan (`Rows Removed by Filter: 19996`) → Bitmap Index Scan → Index Only Scan (`Heap Fetches: 0`). Isolation: no dirty read; non-repeatable @RC (100→200); stable @RR (100→100); phantom @RC (2→3) not @RR (2→2). **Write skew**: −100 @REPEATABLE READ vs rejected @SERIALIZABLE (SQLSTATE 40001, sum held 50). HikariCP size-2 pool: 3rd borrow timed out ~506 ms, recovered on return. Partition pruning: only `txn_2026_02` scanned. Online DDL: CIC→25001 in txn, builds in autocommit; fast default backfills 1000 rows. §12.3 mutation (SERIALIZABLE→REPEATABLE READ) → "Expecting code to raise a throwable" at `WriteSkewTest.java:89`, reverted; `steps/step-10/smoke.sh` PASSED. ADR-0004. |
| `step-09-end` | 🔴 Full | BUILD SUCCESS · 50 tests (+4 cif) | N+1 proven via Hibernate statistics (3 stmts lazy vs 1 with `@EntityGraph`); `@Version` optimistic-lock conflict → `ObjectOptimisticLockingFailureException`; interface projection; §12.3 mutation (remove `@Version`) caught; `steps/step-09/smoke.sh` PASSED |
| `step-08-end` | 🔴 Full | BUILD SUCCESS · 46 tests (+6 cif) | CIF on real Postgres (Testcontainers, random port 57881, PG 17.10); Flyway v1 migrated; live POST→201/GET→200, 400, 404; §12.3 mutation (404→200) caught + reverted; `steps/step-08/smoke.sh` PASSED |
| `step-07-end` | 🟠 Standard | BUILD SUCCESS · 40 tests (+6 spring-lab) | `@Around` audit aspect fires on `@Audited` HTTP calls; `AccountService$$SpringCGLIB$$0` proxy; self-invocation pitfall proven by counter; capstone slice 200/404; `steps/step-07/smoke.sh` PASSED. **Phase A complete.** |
| `step-06-end` | 🟠 Standard | BUILD SUCCESS · 34 tests (+4 spring-lab) | typed `@ConfigurationProperties` binding; custom `GreetingAutoConfiguration` (on/off/back-off); `/actuator/conditions` 141 applied / 82 skipped on hello-service; `steps/step-06/smoke.sh` PASSED |
| `step-05-end` | 🟠 Standard | BUILD SUCCESS · 28 tests (+6 spring-lab) | conditional beans (fixed/market via `@ConditionalOnProperty`), constructor DI, singleton vs prototype scopes, SpEL, lifecycle order 1→4 + `@PreDestroy` in app run; `steps/step-05/smoke.sh` PASSED |
| `step-04-end` | 🟠 Standard | BUILD SUCCESS · 22 tests (+2 jvm) | `javap -c` bytecode; `-Xlog:gc` G1 young pauses; `-XX:+PrintCompilation` C1/C2/OSR; JFR summary; `-Xlog:class+load` (CDS); escape-analysis discovery; `steps/step-04/smoke.sh` PASSED |
| `step-03-end` | 🟠 Standard | BUILD SUCCESS · 20 tests (+4 net) | `LoopbackHttpTest` (JDK HttpServer) round-trips via `HttpClient` + raw socket; `HttpClientDemo`/`RawHttpDemo` vs hello-service; curl -v / nslookup / TLS captures; `steps/step-03/smoke.sh` PASSED |
| `step-02-end` | 🟠 Standard | BUILD SUCCESS · 18 tests (java-basics 16 + hello 2) | `Step2Demo` prints net 1124.50 USD; `steps/step-02/smoke.sh` PASSED; records/sealed/streams/Optional/java.time exercised |
| `step-01-end` | 🟠 Standard | BUILD SUCCESS · 2/2 tests | Tomcat 11.0.21, random test port, `GET /api/hello` → 200 JSON, `/actuator/health` → UP, repackaged jar |
| `step-01-start` | 🟢 Light | BUILD SUCCESS | parent aggregator, no modules yet |

## Known watch-items (carried forward)
- **Spring AI** is RC on the Boot-4 line → re-pin to GA at Phase I (Step 46+), or use the Python FastAPI sidecar path.
- **ErrorProne/NullAway** may not support JDK 25 → verify at Step 28; keep Spotless + Checkstyle regardless.
- **Kubernetes/cloud** are verify-adjacent in this sandbox (no local cluster) → learner installs `kind`; we lint/template/dry-run.

## Pinned facts
See `VERSIONS.md` (versions) and `CAPABILITIES.md` (what runs here). Never `latest`. Money = BigDecimal; time = UTC/Instant.
