# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, `git checkout` the last verified tag, re-runs `make doctor`, then continues
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** A — Foundations 🟢
- **Step:** 1 of 67 — *Setup + CLI/Linux/Git + first running Spring Boot app* — **scaffolding complete; building the app**
- **Last verified tag:** `step-01-start` (scaffold; `./mvnw verify` → BUILD SUCCESS, parent aggregator)
- **Next action:** finish Step 1 — add `services/hello`, prove `./mvnw verify` green, tag `step-01-end`, then start Step 2.

## Done so far
- ✅ **Step 0 — capability preflight** → `CAPABILITIES.md` (JDK 25.0.3 LTS, Maven 3.9.12, Docker running, no local k8s, scanners install-on-demand).
- ✅ **Version set resolved & verified to build** → `VERSIONS.md` + `adr/0002` (Java 25 + Spring Boot 4.0.6 + Spring Cloud 2025.1.1).
- ✅ **Repo scaffold** — parent POM, Maven Wrapper, Makefile, `.gitignore`/`.env.example`/`.editorconfig`/`.tool-versions`, ADRs, README, COURSE.md.
- ✅ **Step 1 lesson** — `steps/step-01/lesson.md` (full §8 contract), `requests.http`, `smoke.sh`.

## Verification ledger (most recent first)
| Tag | Tier | `./mvnw verify` | Notes |
|---|---|---|---|
| `step-01-start` | 🟢 Light | BUILD SUCCESS | parent aggregator, no modules yet |

## Pinned facts
See `VERSIONS.md` (versions) and `CAPABILITIES.md` (what runs here). Never `latest`. Money = BigDecimal; time = UTC/Instant.
