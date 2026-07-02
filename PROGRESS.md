# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, then follows `docs/ai/CONTEXT-PLAYBOOK.md`: confirm you're on `main` (never detached) and the last verified tag builds, re-run `make doctor`, then continue
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** G — DevOps Zero to Hero 🔵🟣 **IN PROGRESS** (Step 33 ✅ of 33–38). Phase F ✅ (29–32) 🎖️. Phase E ✅ (25–28). Phase D ✅ (19–24). Phase C ✅ (13–18). Phase B ✅ (8–12).
- **Step:** 33 of 67 — *Containerize everything (multi-stage · distroless · non-root → Buildpacks/Jib + Compose)* — ✅ **COMPLETE & VERIFIED** (🟠 Standard tier; Lesson DoD: PASS — 12 sub-steps · 🔮 13 · ❓ 9 · 🔬 3 · ▶️ 14).
- **Last verified tag:** `step-33-end` (== `step-34-start`) — full-repo `./mvnw verify` → BUILD SUCCESS (14 modules, 04:18; **zero Java changes** this step) + 🎓 **full-stack capstone 2/2 vs ZERO host services** + smoke.sh 6 gates PASSED.
  The bank is now `make bank-up`: ONE `deploy/Dockerfile.service` (ARG MODULE/PORT; temurin-25 build w/ locked .m2 cache mount → jarmode layered extract → distroless java25 `:nonroot`, MaxRAMPercentage=75 baked) × 7 services + compose profile `bank` (11 containers, initdb 2nd DB `cif`, redpanda dual listeners internal 29092/external 9092, env-only rewiring — Step-15 `${services.*.uri}` placeholders did the gateway). Labs: cgroup heap 25→75%, OOM 137, JDK-25 AOT cache 3.76→2.61 s (not baked — ADR-0024). Buildpacks WORKS on Java 25 (684 MB, benched); **Jib 3.4.5 INCOMPATIBLE** (class file major 69 — VERSIONS watch-item). Hybrid Step-32 topology still works (default profile).
  Full detail → the `step-33-end` row in `docs/ai/VERIFICATION-LEDGER.md`.

## Next action
**Step 34 — Kubernetes** 🔵 — per the step-34 row of `COURSE.md` (grep `| 34 |`): manifests, config/secrets, Actuator probes, `securityContext` + RBAC basics, graceful shutdown, on a REAL local cluster (`kind` v0.32.0 installed — CAPABILITIES 🟢; also teach the cluster-less verify path per the Phase-G note). Lift the Step-33 images (`bab-<svc>:0.1.0-SNAPSHOT`) into the cluster; Boot 4 already exposes `/actuator/health/{liveness,readiness}` (verified at 33) — they become the probe targets; distroless "no exec" meets ephemeral debug containers. kind needs images loaded (`kind load docker-image`) or a local registry — decide in-step. Follow `docs/ai/CONTEXT-PLAYBOOK.md` (read-set: this file → `steps/step-33/capsule.md` → COURSE row 34 → CAPABILITIES/VERSIONS → LESSON-SPEC/PROJECT-MAP). Keep `step-34-end == step-35-start`. **Carry-forward security debt** (honor when relevant): R-001 BOLA, R-002 cif/notification/market-info/onboarding no app-auth, R-003 rate limiting (Step 37), R-005 key rotation (Phase H), R-006 scanning gates (Step 40) — `security/risk-register.md`; plus CONTRACT-DEBT: duplicate `POST /api/accounts` → 500 (map to 409 when touching demand-account).

## Lesson metrics
Measured 2026-07-02 after full reconciliation (enrichment loops merged + ADHD layer + aids pass). 🔬 counts `🔬 **Break` break-its only; ▶️ counts run-and-see markers. Compare new steps against these — a dive below the neighborhood means decay.

| Step | lesson lines | sub-steps | 🔮 | ❓ | 🔬 | ▶️ |
|---|---|---|---|---|---|---|
| 1 | 1365 | 8 | 8 | 5 | 0 | 8 |
| 2 | 2039 | 12 | 12 | 4 | 3 | 12 |
| 3 | 1388 | 6 | 5 | 3 | 1 | 6 |
| 4 | 1456 | 7 | 7 | 3 | 0 | 3 |
| 5 | 1711 | 11 | 10 | 3 | 1 | 4 |
| 6 | 1492 | 6 | 6 | 3 | 4 | 7 |
| 7 | 1646 | 7 | 7 | 3 | 3 | 8 |
| 8 | 2093 | 12 | 7 | 3 | 2 | 7 |
| 9 | 1697 | 6 | 6 | 3 | 2 | 3 |
| 10 | 1467 | 7 | 7 | 3 | 0 | 7 |
| 11 | 1066 | 4 | 3 | 3 | 1 | 4 |
| 12 | 2665 | 12 | 12 | 4 | 2 | 11 |
| 13 | 1683 | 8 | 8 | 3 | 1 | 8 |
| 14 | 1813 | 9 | 11 | 3 | 1 | 5 |
| 15 | 1461 | 10 | 8 | 3 | 1 | 4 |
| 16 | 1692 | 10 | 10 | 3 | 1 | 10 |
| 17 | 2305 | 12 | 11 | 4 | 0 | 12 |
| 18 | 2066 | 11 | 11 | 3 | 2 | 11 |
| 19 | 1978 | 11 | 11 | 4 | 4 | 11 |
| 20 | 3298 | 15 | 14 | 3 | 0 | 15 |
| 21 | 2839 | 14 | 14 | 5 | 1 | 14 |
| 22 | 2068 | 14 | 11 | 5 | 1 | 15 |
| 23 | 2812 | 14 | 14 | 4 | 1 | 14 |
| 24 | 2646 | 11 | 9 | 4 | 1 | 12 |
| 25 | 2555 | 11 | 10 | 6 | 1 | 10 |
| 26 | 2894 | 14 | 13 | 5 | 1 | 15 |
| 27 | 1359 | 9 | 10 | 3 | 2 | 8 |
| 28 | 1743 | 11 | 11 | 3 | 0 | 11 |
| 29 | 1617 | 6 | 6 | 3 | 0 | 6 |
| 30 | 1426 | 9 | 9 | 4 | 0 | 9 |
| 31 | 2143 | 9 | 6 | 5 | 3 | 9 |
| 32 | 2645 | 11 | 11 | 7 | 2 | 13 |
| 33 | 1981 | 12 | 13 | 9 | 3 | 14 |

## Known watch-items (carried forward)
- **Spring AI** is RC on the Boot-4 line → re-pin to GA at Phase I (Step 46+), or use the Python FastAPI sidecar path.
- **Auth session store is in-memory** (Step 32): a `docker compose restart auth` signs everyone out; horizontal scale needs a shared store (Redis) — revisit in Phase G/H.
- **Jib 3.4.5 (newest) INCOMPATIBLE with JDK 25** (Step 33: `class file major version 69` — same ASM lag as PITest 1.19.1). Hand-written Dockerfile is canonical (ADR-0024); re-probe Jib on new releases. Buildpacks verified working (benched).
- **AOT cache not in fleet images** (Step 33): training runs need live infra (Flyway would fail `docker build`) — revisit in CI (Step 35+).
- **kind v0.32.0 IS installed** (CAPABILITIES 🟢): Step 34 runs a REAL local cluster; only managed-cloud stays verify-adjacent. `make` IS present too (4.4.1 — CAPABILITIES updated at 33).
- **Gateway route order is load-bearing** (Step 32): the SPA catch-all must stay LAST; guarded by `serviceRoutesStillWinOverTheCatchAll`. Corollary found at 33: `/actuator/gateway` doesn't exist on the MVC gateway → unknown paths return the SPA's HTML with 200 (step-15 claim → IMPROVEMENT-BACKLOG WO-15.A).
- **First `--profile bank` up needs an empty pg volume** (initdb creates the `cif` DB) — `make bank-down` does the `down -v`.

## Pointers
- Full verification history → `docs/ai/VERIFICATION-LEDGER.md`
- How to generate the next step → `docs/ai/CONTEXT-PLAYBOOK.md`
- Capability matrix → `CAPABILITIES.md`
- Pinned versions → `VERSIONS.md`
