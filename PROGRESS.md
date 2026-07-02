# 📍 PROGRESS.md — resume state

> A fresh session reads THIS file first, then follows `docs/ai/CONTEXT-PLAYBOOK.md`: confirm you're on `main` (never detached) and the last verified tag builds, re-run `make doctor`, then continue
> from **Next action**. A single session will not reach Step 67 — that is expected, not a failure.

## Where we are
- **Phase:** F — Full-Stack Frontend 🔵 **IN PROGRESS** (Steps 29–32). Phase E ✅ (25–28). Phase D ✅ (19–24). Phase C ✅ (13–18). Phase B ✅ (8–12).
- **Step:** 30 of 67 — *Frontend pt.2 — state, data & forms (TanStack Query · RHF+Zod · SSE)* — ✅ **COMPLETE & VERIFIED** (🟠 Standard tier — frontend feature work + a small gateway route).
- **Last verified tag:** `step-30-end` (== `step-31-start`) — full-repo `./mvnw verify` → BUILD SUCCESS (**14 modules**, 3:14; gates green; gateway **5 tests** incl. the new notification route) + the SPA's npm build/lint/**15 Vitest tests** green.
  TanStack Query data layer (`useAccount`/`useEntries`/`useTransfer`) + RHF/Zod **TransferForm** + **SSE** `useNotificationStream`/`LiveNotifications`; gateway now fronts `/notifications/**`. smoke.sh PASSED; clean-room `npm ci` green; ADR-0021.
  Full detail (deps pinned, §12.3 mutation, §12.8 verify-adjacent notes) → the `step-30-end` row in `docs/ai/VERIFICATION-LEDGER.md`.

## Next action
**Step 31 — Frontend pt.3 (testing & accessibility)** 🔵 — per the step-31 row of `COURSE.md`: component tests (**Testing Library**, deepen), **E2E (Playwright)**, **API mocking (MSW)**, **WCAG accessibility basics**, **i18n + multi-currency formatting**. Build on the Step-30 `frontend/`: introduce **MSW** to mock the gateway at the network layer (replace the per-test `vi.mock('../api/client')` with realistic request handlers); add **Playwright** E2E (login → transfer → see balance/live update — note Playwright needs browsers; `npx playwright install` may be a sandbox capability question → check & document per §12.8, likely verify-adjacent for the actual browser run); **a11y** (labels/roles/focus, maybe `axe`/`jest-axe`); **i18n** (e.g. `react-i18next`) + `Intl.NumberFormat` multi-currency money formatting. Likely 🟠 Standard. **⚠️ Capability check:** Playwright browser download/run in this sandbox — if it can't launch a browser, write the E2E specs + document them as verify-adjacent, and fully verify the Testing-Library/MSW/a11y/i18n parts. Follow `docs/ai/LESSON-SPEC.md` (the distilled §8/§8.1/§12 contract — do not read the master prompt); resume from `step-31-start` (== `step-30-end`); keep `step-31-end == step-32-start`. Then Step 32 (token refresh + route-guard hardening, bundle/perf, **Dockerize the SPA + serve via gateway/CDN**, deploy). **Carry-forward security debt** (honor when relevant): R-001 BOLA, R-002 cif/notification/market-info/onboarding no app-auth, R-003 rate limiting (Step 37), R-005 key rotation (Phase H), R-006 scanning gates (Step 40) — all in `security/risk-register.md`. Frontend JWT-in-localStorage XSS hardening = **Step 32**.

## Lesson metrics
Measured 2026-07-02 after the pure-edit improvement pass (🔬 counts `🔬 **Break` break-its only; ▶️ counts run-and-see markers — zeros on steps 18–30 reflect the thin-build backlog in `docs/ai/audit/IMPROVEMENT-BACKLOG.md`, not a healthy state).

| Step | lesson lines | sub-steps | 🔮 | ❓ | 🔬 | ▶️ |
|---|---|---|---|---|---|---|
| 1 | 1319 | 8 | 6 | 5 | 1 | 7 |
| 2 | 2043 | 12 | 12 | 4 | 3 | 13 |
| 3 | 1357 | 6 | 5 | 3 | 1 | 6 |
| 4 | 1391 | 7 | 7 | 3 | 0 | 3 |
| 5 | 1664 | 11 | 10 | 3 | 1 | 4 |
| 6 | 1435 | 6 | 6 | 3 | 4 | 7 |
| 7 | 1587 | 7 | 7 | 3 | 3 | 8 |
| 8 | 2038 | 12 | 7 | 3 | 2 | 7 |
| 9 | 1671 | 6 | 6 | 3 | 2 | 3 |
| 10 | 1502 | 7 | 7 | 3 | 2 | 6 |
| 11 | 1010 | 4 | 3 | 3 | 1 | 2 |
| 12 | 2363 | 7 | 4 | 3 | 1 | 2 |
| 13 | 988 | 7 | 3 | 3 | 0 | 4 |
| 14 | 1510 | 7 | 6 | 3 | 1 | 2 |
| 15 | 1038 | 6 | 4 | 3 | 1 | 2 |
| 16 | 1349 | 6 | 3 | 3 | 1 | 2 |
| 17 | 1023 | 5 | 3 | 3 | 1 | 3 |
| 18 | 804 | 5 | 3 | 3 | 2 | 0 |
| 19 | 463 | 5 | 4 | 3 | 1 | 5 |
| 20 | 1385 | 4 | 3 | 3 | 0 | 0 |
| 21 | 483 | 5 | 4 | 3 | 0 | 0 |
| 22 | 461 | 3 | 3 | 4 | 0 | 0 |
| 23 | 435 | 3 | 3 | 3 | 0 | 0 |
| 24 | 1056 | 7 | 3 | 3 | 0 | 3 |
| 25 | 403 | 4 | 3 | 3 | 1 | 0 |
| 26 | 770 | 4 | 3 | 3 | 0 | 0 |
| 27 | 674 | 4 | 3 | 3 | 0 | 0 |
| 28 | 583 | 6 | 4 | 3 | 1 | 0 |
| 29 | 1364 | 6 | 6 | 3 | 1 | 0 |
| 30 | 460 | 5 | 3 | 3 | 1 | 0 |

## Known watch-items (carried forward)
- **Spring AI** is RC on the Boot-4 line → re-pin to GA at Phase I (Step 46+), or use the Python FastAPI sidecar path.
- **Frontend JWT storage** (Step 29): JWT in `localStorage` (XSS-exposed) — teaching simplification; harden in **Step 32** (httpOnly cookie / in-memory + refresh rotation).
- **Kubernetes/cloud** are verify-adjacent here (no local cluster) → learner installs `kind`; we lint/template/dry-run.
- **Live browser** flows are verify-adjacent (no browser in sandbox) — headless CORS preflight + Vitest instead (§12.8).

## Pointers
- Full verification history → `docs/ai/VERIFICATION-LEDGER.md`
- How to generate the next step → `docs/ai/CONTEXT-PLAYBOOK.md`
- Capability matrix → `CAPABILITIES.md`
- Pinned versions → `VERSIONS.md`
