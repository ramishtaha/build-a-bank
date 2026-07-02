# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, then follows `docs/ai/CONTEXT-PLAYBOOK.md`: confirm you're on `main` (never detached) and the last verified tag builds, re-run `make doctor`, then continue
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** F — Full-Stack Frontend ✅ **COMPLETE** (Steps 29–32) 🎖️ full-stack milestone. Phase E ✅ (25–28). Phase D ✅ (19–24). Phase C ✅ (13–18). Phase B ✅ (8–12).
- **Step:** 32 of 67 — *Frontend pt.4 — hardening & ship (refresh rotation · code-splitting · SPA behind the gateway · 🎓 capstone)* — ✅ **COMPLETE & VERIFIED** (🔴 Full tier; Lesson DoD: PASS — 11 sub-steps · 🔮 11 · ❓ 7 · 🔬 2 · ▶️ 13).
- **Last verified tag:** `step-32-end` (== `step-33-start`) — full-repo `./mvnw verify` → BUILD SUCCESS (14 modules; auth 17 tests, gateway 7, demand-account 48) + SPA: split build (login path 100.3 kB gzip, was 127.8) / lint (0 errors) / **29 Vitest** / hermetic E2E 2/2 / 🎓 **full-stack capstone E2E 2/2 (zero mocks, real gateway :8080)** / smoke.sh 6 gates PASSED.
  Session hardening: in-memory access JWT (TTL 10 min) + rotating httpOnly refresh cookie with reuse detection (replay→family revoked; 3 s race grace→409) — localStorage debt RETIRED (risk C-10). SPA shipped: nginx container behind gateway `Path=/**` catch-all (order-regression-tested), one origin, credentialed-CORS wildcard guard. Capstone caught 2 real bugs (currency:null since ~Step 13 masked by MSW; proxied-CORS 403). §12.3 mutation both sides. ADR-0023.
  Full detail → the `step-32-end` row in `docs/ai/VERIFICATION-LEDGER.md`.

## Next action
**Step 33 — Phase G opens (DevOps zero-to-hero)** 🔵🟣 — per the step-33 row of `COURSE.md` (grep `| 33 |`) and the Phase-G intro. Likely: containerize the JAVA services (the SPA's multi-stage `frontend/Dockerfile` from Step 32 is the template; `deploy/compose.fullstack.yaml` grows from infra-only toward full-compose) — read the COURSE row first, don't assume. `kind` v0.32.0 IS installed for the k8s steps (CAPABILITIES.md 🟢). Follow `docs/ai/CONTEXT-PLAYBOOK.md` (read-set: this file → `steps/step-32/capsule.md` → COURSE row 33 → CAPABILITIES/VERSIONS → LESSON-SPEC/PROJECT-MAP). Keep `step-33-end == step-34-start`. **Carry-forward security debt** (honor when relevant): R-001 BOLA, R-002 cif/notification/market-info/onboarding no app-auth, R-003 rate limiting (Step 37), R-005 key rotation (Phase H), R-006 scanning gates (Step 40) — `security/risk-register.md`; plus CONTRACT-DEBT: duplicate `POST /api/accounts` → 500 (map to 409 when touching demand-account).

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

## Known watch-items (carried forward)
- **Spring AI** is RC on the Boot-4 line → re-pin to GA at Phase I (Step 46+), or use the Python FastAPI sidecar path.
- ~~Frontend JWT storage~~ ✅ **RETIRED at Step 32** (in-memory + rotating httpOnly refresh cookie; risk register C-10).
- **Auth session store is in-memory** (Step 32): an auth restart signs everyone out; horizontal scale needs a shared store (Redis) — revisit in Phase G/H.
- **kind v0.32.0 IS installed** (CAPABILITIES 🟢): Phase G runs a REAL local cluster; only managed-cloud stays verify-adjacent.
- **Gateway route order is load-bearing** (Step 32): the SPA catch-all must stay the LAST route; guarded by `serviceRoutesStillWinOverTheCatchAll`.

## Pointers
- Full verification history → `docs/ai/VERIFICATION-LEDGER.md`
- How to generate the next step → `docs/ai/CONTEXT-PLAYBOOK.md`
- Capability matrix → `CAPABILITIES.md`
- Pinned versions → `VERSIONS.md`
