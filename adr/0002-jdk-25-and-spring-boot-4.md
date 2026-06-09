# ADR-0002: Pin Java 25 LTS + Spring Boot 4.0.6 as the platform baseline

- **Status:** Accepted
- **Date:** 2026-06-09
- **Deciders:** Build-a-Bank (autonomous senior default) · supersedes nothing

## Context
Step 0 (capability preflight) found **Oracle JDK 25.0.3 LTS** installed (the latest LTS present) and verified network
access to Maven Central. The Operating Contract says: pin the *latest LTS present*, pin the *latest stable* Spring Boot,
and **verify the set resolves and builds together** before relying on it. Candidate decision points:

1. **Java 25 vs 21** — both are LTS. 25 is present and newest.
2. **Spring Boot 4.0.6 (GA) vs 3.5.14 (GA)** — Boot 4 is what the frontier phase targets; risk is ecosystem lag.
3. **Ecosystem readiness** — Spring Cloud Gateway is needed as early as Step 15, so its Boot-4 train must be GA.

Verified facts (Maven Central metadata, 2026-06-09):
- Latest **GA** Spring Boot is **4.0.6** (4.1.0 is still RC).
- **Spring Cloud 2025.1.1 GA** exists for the Boot-4.0 line → Gateway/Config/Stream/Feign available. ✅
- Testcontainers 2.0.5, Flyway 12.8.1, ArchUnit 1.4.2, Spring Modulith 2.0.6, Resilience4j 2.4.0 — all GA. ✅
- **Spring AI** on the Boot-4 line is only **2.0.0-RC1** (1.1.7 GA is Boot-3). ⚠️ Phase I concern (far away).

## Decision
Pin **Java 25.0.3 LTS** and **Spring Boot 4.0.6** with **Spring Cloud 2025.1.1** as the baseline for the *entire*
course (not a late-phase migration). Build directly on Boot 4 to avoid a mid-course rewrite; teach the 2→3→4 history
in the Domain-14 "Then vs Now" sections rather than by actually shipping on Boot 3 first.

**Proof:** a minimal `services/hello` app compiles with `--release 25` and `./mvnw verify` is green (2/2 tests),
the packaged jar boots embedded Tomcat 11.0.21 and serves `GET /api/hello` → 200. See `steps/step-01` Verification Log.

## Consequences
- ✅ One consistent, modern baseline; no Boot-3→4 migration churn across 67 steps.
- ✅ Frontier phase (Step 59) becomes "what's *new beyond* our baseline" rather than "finally upgrade."
- ⚠️ **Spring AI**: re-pin to GA at Phase I, or use the Python FastAPI sidecar path; documented in `VERSIONS.md`.
- ⚠️ **ErrorProne/NullAway** may not support JDK 25 yet — verify at Step 28; keep Spotless+Checkstyle regardless.
- ⚠️ **`TestRestTemplate` was removed in Boot 4** — we use `RestClient`/`RestTestClient`/`MockMvcTester` instead.
- 🔁 Re-validate the whole set if we bump Boot (e.g. to 4.1 GA); `VERSIONS.md` is the single source of truth.
