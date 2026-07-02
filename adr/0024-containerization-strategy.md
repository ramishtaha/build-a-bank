# ADR-0024 — Containerization strategy: one parameterized Dockerfile, distroless non-root runtime, compose profile topology

Date: 2026-07-03 · Status: Accepted · Step: 33 (Phase G opens)

## Context

Phase F ended with a hybrid topology: infra + SPA in containers (`deploy/compose.fullstack.yaml`),
the Java services on the host via `./mvnw spring-boot:run`. Phase G needs the whole platform as
images — for Compose now, Kubernetes (Step 34) and CI/CD (Step 35) next. Six Spring Boot services +
the gateway differ only by Maven module and port. Options considered: per-service Dockerfiles (7
near-identical files to keep in sync), Cloud Native Buildpacks (`spring-boot:build-image` — zero
Dockerfile, but opaque for teaching and heavyweight per build), Jib (daemonless, config-in-pom).

## Decision

1. **One parameterized `deploy/Dockerfile.service`** (`ARG MODULE`, `ARG PORT`), context = repo root
   (the Maven reactor needs every module pom). Three stages: build (temurin-25-jdk-alpine +
   `./mvnw -pl $MODULE -am package`, BuildKit `--mount=type=cache,target=/root/.m2,sharing=locked` —
   `locked` because compose builds services concurrently and Maven's local repo has no file locking)
   → extract (`java -Djarmode=tools … extract --layers --launcher`) → run.
2. **Runtime = `gcr.io/distroless/java25-debian13:nonroot`** (uid 65532, no shell, no package
   manager). Measured honestly: ~same *size* as `eclipse-temurin:25-jre-alpine` (360 vs 359 MB) —
   the win is attack surface, not megabytes. Boot's four extracted layers are COPYied least→most
   volatile so a code-only change rebuilds one small layer.
3. **`-XX:MaxRAMPercentage=75.0` via `JAVA_TOOL_OPTIONS`** baked into the image; every compose
   service carries an explicit `mem_limit` (the JVM sizes its heap from the cgroup limit).
4. **One compose file, two topologies via profiles**: the default profile keeps the Step-32 hybrid
   (infra + SPA, services on host); `--profile bank` adds the 7 containerized services. Redpanda
   gains the dual-listener split (internal `redpanda:29092` for containers, external
   `localhost:9092` unchanged for the host) so both topologies coexist. Postgres provisions the
   second database (cif) via `/docker-entrypoint-initdb.d`.
5. **Buildpacks and Jib are taught as alternatives** (with a real `spring-boot:build-image` run),
   not adopted: the course optimizes for "nothing is magic", and one visible Dockerfile beats a
   generated image for that goal. Revisit if/when a registry pipeline (Step 35) makes their
   caching/daemonless properties decisive.

## Consequences

- `step-33`+ can `docker compose --profile bank up` the entire bank on one origin (:8080) — the
  Step-32 full-stack Playwright capstone now runs against an all-container topology, zero host services.
- Distroless kills exec-style debugging and in-container compose healthchecks (no shell, no wget) —
  app containers use `depends_on: service_started` + lazy clients; Kubernetes HTTP probes (Step 34)
  restore proper readiness gating from OUTSIDE the container.
- AOT cache (JDK 25, measured ~30% faster startup on auth) is NOT baked into images: the training
  run happens at image-build time, when DB-backed services can't reach infra (Flyway would fail the
  build). Documented as a lab; revisit in CI (Step 35+) where a training environment can exist.
- No `curl`/shell in runtime images ⇒ container healthchecks live at the infra layer only; the
  gateway's `/actuator/health` is the platform's aliveness signal until k8s probes arrive.
