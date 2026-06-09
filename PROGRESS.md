# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, `git checkout` the last verified tag, re-runs `make doctor`, then continues
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** A — Foundations 🟢
- **Step:** 1 of 67 — *Setup + CLI/Linux/Git + first running Spring Boot app* — ✅ **COMPLETE & VERIFIED**
- **Last verified tag:** `step-01-end` (== `step-02-start`) — `./mvnw verify` → BUILD SUCCESS, 2/2 tests, app serves `GET /api/hello` 200.
- **Next action:** **Step 2 — Java language primer** (syntax → OOP → generics → collections → streams/lambdas → `Optional` → records/sealed → exceptions → `java.time`). Build it in a small, runnable `playground`/learning module; keep `step-02-end == step-03-start`.

## Done so far
- ✅ **Step 0 — capability preflight** → `CAPABILITIES.md` (JDK 25.0.3 LTS, Maven 3.9.12, Docker running, no local k8s, scanners install-on-demand).
- ✅ **Version set resolved & verified to build** → `VERSIONS.md` + `adr/0002` (Java 25 + Spring Boot 4.0.6 + Spring Cloud 2025.1.1; Spring AI flagged for re-pin at Phase I).
- ✅ **Repo scaffold** — parent POM, Maven Wrapper, Makefile, `.gitignore`/`.env.example`/`.editorconfig`/`.tool-versions`, ADR-0001/0002, README, COURSE.md.
- ✅ **Step 1** — `steps/step-01/lesson.md` (full §8 contract), `requests.http`, `smoke.sh`, and the working `services/hello` app. Tagged `step-01-start` (scaffold) and `step-01-end` (verified green).

## Verification ledger (most recent first)
| Tag | Tier | `./mvnw verify` | Proof |
|---|---|---|---|
| `step-01-end` | 🟠 Standard | BUILD SUCCESS · 2/2 tests | Tomcat 11.0.21, random test port, `GET /api/hello` → 200 JSON, `/actuator/health` → UP, repackaged jar |
| `step-01-start` | 🟢 Light | BUILD SUCCESS | parent aggregator, no modules yet |

## Known watch-items (carried forward)
- **Spring AI** is RC on the Boot-4 line → re-pin to GA at Phase I (Step 46+), or use the Python FastAPI sidecar path.
- **ErrorProne/NullAway** may not support JDK 25 → verify at Step 28; keep Spotless + Checkstyle regardless.
- **Kubernetes/cloud** are verify-adjacent in this sandbox (no local cluster) → learner installs `kind`; we lint/template/dry-run.

## Pinned facts
See `VERSIONS.md` (versions) and `CAPABILITIES.md` (what runs here). Never `latest`. Money = BigDecimal; time = UTC/Instant.
