# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, `git checkout` the last verified tag, re-runs `make doctor`, then continues
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** A — Foundations 🟢 (building Steps 2–7 continuously this phase)
- **Step:** 4 of 67 — *How Java Runs: the JVM Up Close* — ✅ **COMPLETE & VERIFIED**
- **Last verified tag:** `step-04-end` (== `step-05-start`) — `./mvnw verify` → BUILD SUCCESS, 22 tests; javap/GC/JIT/JFR/classloading captured; smoke green.
- **Next action:** **Step 5 — Spring Core & IoC deep** (bean lifecycle, scopes, `@Bean` vs `@Component`, conditional beans, `@Profile`, SpEL, dependency injection — the IoC container demystified). Buildable artifact: NEW module `playground/spring-lab` (per ADR-0003) — a `spring-boot-starter` (no web yet) app with several beans demonstrating constructor DI, scopes (singleton vs prototype), `@Bean` factory methods, `@Conditional`/`@Profile`, a `BeanPostProcessor`, and a `CommandLineRunner` that prints the lifecycle; tests with `ApplicationContextRunner`. Keep `step-05-end == step-06-start`.

## Done so far
- ✅ **Step 0 — capability preflight** → `CAPABILITIES.md` (JDK 25.0.3 LTS, Maven 3.9.12, Docker running, no local k8s, scanners install-on-demand).
- ✅ **Version set resolved & verified to build** → `VERSIONS.md` + `adr/0002` (Java 25 + Spring Boot 4.0.6 + Spring Cloud 2025.1.1; Spring AI flagged for re-pin at Phase I).
- ✅ **Repo scaffold** — parent POM, Maven Wrapper, Makefile, `.gitignore`/`.env.example`/`.editorconfig`/`.tool-versions`, ADR-0001/0002, README, COURSE.md.
- ✅ **Step 1** — `steps/step-01/lesson.md` (full §8 contract), `requests.http`, `smoke.sh`, and the working `services/hello` app. Tagged `step-01-start` (scaffold) and `step-01-end` (verified green).

## Verification ledger (most recent first)
| Tag | Tier | `./mvnw verify` | Proof |
|---|---|---|---|
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
