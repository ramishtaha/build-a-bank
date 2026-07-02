# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, then follows `docs/ai/CONTEXT-PLAYBOOK.md`: confirm you're on `main` (never detached) and the last verified tag builds, re-run `make doctor`, then continue
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** F — Full-Stack Frontend 🔵 **IN PROGRESS** (Steps 29–32). Phase E ✅ (25–28). Phase D ✅ (19–24). Phase C ✅ (13–18). Phase B ✅ (8–12).
- **Step:** 31 of 67 — *Frontend pt.3 — testing, a11y & i18n (MSW · Playwright · axe · Intl)* — ✅ **COMPLETE & VERIFIED** (🟠 Standard tier; Lesson DoD: PASS — 9 sub-steps · 🔮 6 · ❓ 5 · 🔬 3 · ▶️ 9).
- **Last verified tag:** `step-31-end` (== `step-32-start`) — full-repo `./mvnw verify` → BUILD SUCCESS (14 modules) + SPA: npm build (133 modules) / lint (0 errors) / **24 Vitest tests** (MSW floor) / **Playwright E2E 2/2 in real Chromium** / smoke.sh PASSED.
  MSW network-floor mocking + axe a11y + i18n (en/es) + `Intl` multi-currency + **hermetic Playwright E2E** (login→balance→transfer→live SSE; Idempotency-Key asserted on the wire; §12.3 mutation both directions). ADR-0022.
  Full detail → the `step-31-end` row in `docs/ai/VERIFICATION-LEDGER.md`.

## Next action
**Step 32 — Frontend pt.4 (hardening & ship)** 🔵 — per the step-32 row of `COURSE.md`: **token refresh & route guards** (move the JWT out of `localStorage` — httpOnly cookie or in-memory + refresh rotation; the security debt carried since Step 29), **bundle/perf optimization** (code-splitting, lazy routes, bundle analysis — current single chunk is 414.72 kB), **Dockerize the SPA + serve via gateway/CDN**, deploy, end-to-end demo. 🎓 **Phase-F capstone closes here**: the full money-transfer UI — form + validation → live balance via SSE → **Playwright E2E against the real stack behind the gateway** (browsers ARE installed — capability re-verified 2026-07-02; the hermetic Step-31 E2E graduates to full-stack). Likely 🔴 Full tier (security path + phase capstone + milestone Step 32). Follow `docs/ai/CONTEXT-PLAYBOOK.md` (read-set: this file → `steps/step-31/capsule.md` → COURSE row 32 → CAPABILITIES/VERSIONS → LESSON-SPEC/PROJECT-MAP). Keep `step-32-end == step-33-start`. **Carry-forward security debt** (honor when relevant): R-001 BOLA, R-002 cif/notification/market-info/onboarding no app-auth, R-003 rate limiting (Step 37), R-005 key rotation (Phase H), R-006 scanning gates (Step 40) — `security/risk-register.md`.

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

## Known watch-items (carried forward)
- **Spring AI** is RC on the Boot-4 line → re-pin to GA at Phase I (Step 46+), or use the Python FastAPI sidecar path.
- **Frontend JWT storage** (Step 29): JWT in `localStorage` (XSS-exposed) — teaching simplification; harden in **Step 32** (httpOnly cookie / in-memory + refresh rotation).
- **Kubernetes/cloud** are verify-adjacent here (no local cluster) → learner installs `kind`; we lint/template/dry-run.
- **Playwright browsers ARE installed here** (re-verified 2026-07-02; hermetic E2E ran green in Step 31). The remaining verify-adjacent browser item is the *full-stack* flow through the real gateway — Step 32's capstone runs it for real.

## Pointers
- Full verification history → `docs/ai/VERIFICATION-LEDGER.md`
- How to generate the next step → `docs/ai/CONTEXT-PLAYBOOK.md`
- Capability matrix → `CAPABILITIES.md`
- Pinned versions → `VERSIONS.md`
