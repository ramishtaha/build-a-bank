# 🧳 Capsule — Step 33

**Exists now:** 14 modules + the WHOLE bank containerized: `make bank-up` = compose `--profile bank` → 11 containers (7 distroless non-root Java images from ONE `deploy/Dockerfile.service` + spa + pg/redis/redpanda), one origin :8080, zero host services. Hybrid Step-32 topology still works (default profile). Tag `step-33-end`.

**This step added:**
- `deploy/Dockerfile.service` (ARG MODULE/PORT): temurin-25-jdk-alpine build w/ `--mount=type=cache,target=/root/.m2,sharing=locked` → `jarmode=tools extract --layers --launcher` → `gcr.io/distroless/java25-debian13:nonroot` (uid 65532), `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0` baked
- root `.dockerignore` (excludes `**/target` — hermetic; docs/steps churn can't bust COPY cache)
- compose: profile `bank` (7 services, env-only wiring, `mem_limit: 512m` each), pg initdb second DB `cif`, redpanda DUAL listeners (internal `redpanda:29092` / external `localhost:9092` unchanged)
- Makefile `bank-up/-down/-ps/-logs`, `image-service`; `steps/step-33/{smoke.sh,requests.http}`; ADR-0024
- Labs (no repo files): JVM-vs-cgroup (25%→75%, OOM 137), JDK-25 AOT cache (auth 3.76→2.61 s)
- ZERO Java changes — Step-8 env config + Step-15 `${services.*.uri}` placeholders did all the work

**Gotchas:**
- Jib 3.4.5 (newest) FAILS on JDK 25 (`class file major version 69`) — same ASM lag as PITest 1.19.1; VERSIONS.md row
- `/actuator/gateway` does NOT exist on the WebMVC gateway → falls through to SPA catch-all (HTML 200 = you fell through); step-15 lesson claim stale (IMPROVEMENT-BACKLOG)
- initdb runs ONLY on empty volume → `bank-down` (does `down -v`) before first `--profile bank` up
- compose `down` without `--profile bank` leaves the 7 bank containers running (profiles filter every verb)
- gateway env names: `SERVICES_DEMAND_ACCOUNT_URI` (dots AND dashes → underscores)
- distroless = no shell → no exec-style healthchecks; app `depends_on` uses `service_started` (k8s probes fix it in 34); Boot 4 exposes `/actuator/health/{liveness,readiness}` by default — Step 34's probe targets

**Callback hooks:** buildpacks run works on Java 25 (Liberica, 684 MB, SBOM layer) — benched per ADR-0024; capstone `npm run test:e2e:fullstack` green vs all-container topology; images `bab-<svc>:0.1.0-SNAPSHOT` (auth 360 MB … demand-account 503 MB); auth restart still drops sessions (in-memory).

**Next step starts:** `step-33-end` == `step-34-start`; full `./mvnw verify` 14× SUCCESS + smoke 6 gates + capstone 2/2. Step 34 = Kubernetes on kind v0.32.0 (installed, CAPABILITIES 🟢): lift THESE images into a cluster; probes ← the health groups above.
