# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, `git checkout` the last verified tag, re-runs `make doctor`, then continues
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** A — Foundations 🟢 — ✅ **COMPLETE** (Steps 1–7 all done & verified)
- **Step:** 7 of 67 — *AOP & the Proxy Model* — ✅ **COMPLETE & VERIFIED** (incl. the 🎓 Phase A Capstone vertical slice)
- **Last verified tag:** `step-07-end` (== `step-08-start`) — full reactor `./mvnw verify` → BUILD SUCCESS, **40 tests**; capstone slice serves 200/404; audit aspect + CGLIB proxy + self-invocation pitfall all proven; `steps/step-07/smoke.sh` PASSED.
- **Next action:** ⏸️ **PAUSED at the Phase A boundary — awaiting user "continue".** Next phase is **Phase B — Data, Databases, Concurrency & Transactions 🔵 (Steps 8–12)**, beginning with **Step 8 — CIF service + Spring Data JPA + persistence context + Flyway + Bean Validation + `@DataJpaTest` with Testcontainers (real Postgres)**. This is the first REAL microservice (`services/cif`) — Docker IS required from here (Testcontainers spins up Postgres). Keep `step-08-end == step-09-start`.

## Done so far
- ✅ **Step 0 — capability preflight** → `CAPABILITIES.md` (JDK 25.0.3 LTS, Maven 3.9.12, Docker running, no local k8s, scanners install-on-demand).
- ✅ **Version set resolved & verified to build** → `VERSIONS.md` + `adr/0002` (Java 25 + Spring Boot 4.0.6 + Spring Cloud 2025.1.1; Spring AI flagged for re-pin at Phase I).
- ✅ **Repo scaffold** — parent POM, Maven Wrapper, Makefile, `.gitignore`/`.env.example`/`.editorconfig`/`.tool-versions`, ADR-0001/0002, README, COURSE.md.
- ✅ **Step 1** — `steps/step-01/lesson.md` (full §8 contract), `requests.http`, `smoke.sh`, and the working `services/hello` app. Tagged `step-01-start` (scaffold) and `step-01-end` (verified green).

## Verification ledger (most recent first)
| Tag | Tier | `./mvnw verify` | Proof |
|---|---|---|---|
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
