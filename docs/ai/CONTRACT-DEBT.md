# CONTRACT-DEBT.md — promised-but-not-shipped register

> Any artifact the course contract promises that a step shipped without gets a row **at the moment it's skipped** (master prompt §12.10). Silent omission is a §12 violation; an honest debt line is not. Review at each phase capstone: pay down or formally descope.

| Step(s) | Artifact | Reason skipped | Remediation plan | Status |
|---|---|---|---|---|
| 02–30 | `solutions/step-NN/` stretch-goal reference solutions (only step-01 exists) | Sessions prioritized verified code + lessons | **Descoped 2026-07-02:** the enrichment passes replaced every dangling `solutions/` pointer with in-lesson design-hint `<details>` (steps 19–27 explicitly; audit fixers rerouted the rest). Inline hints are now the canonical form; folders only if a future capstone warrants one | ✅ descoped |
| 01–30 | `concepts/intellij-idea.md` living IDE doc | Never created; IDE tips were inlined per-lesson instead | Either create once and grow from Step 33 on, or descope (inline 💡 asides are working) | 🔶 open (backfilled) |
| 01–30 | Bruno/Postman collections per step | `requests.http` + curl shipped instead | Descoped by contract right-sizing (2026-07-02): Bruno/Postman now optional | ✅ descoped |
| 01–30 | `seed/` demo datasets + `make seed-NN` | Steps used test fixtures/Flyway data instead | Descoped to optional (2026-07-02); revisit for UI-heavy steps 31–32 where demo data pays off | ✅ descoped |
| 24 | `steps/step-24/requests.http` | Batch step — no new HTTP endpoints claimed, file never created | Add a minimal file (trigger-job + actuator queries) or note "no endpoints this step" in the lesson | 🔶 open |
| 29, 30 (+13 lab/theory steps) | `steps/step-NN/requests.http` | 15 steps lack the file; 29/30 DO expose endpoints (gateway auth route, `/bank` data routes, SSE) — the rest are lab/theory steps with no endpoints | Add files for 29/30 during the Step-31/32 frontend work (same endpoints exercised anyway); lab/theory steps: note "no endpoints" and descope | 🔶 open |
| 01–30 | `steps/step-NN/capsule.md` context capsules | Capsule requirement introduced 2026-07-02 (this audit) | Backfilled for all of steps 1–30 from lesson recaps (2026-07-02); new steps write them natively | ✅ paid |

| 32 | duplicate `POST /api/accounts` returns 500 (unhandled unique-constraint violation) | discovered by the full-stack capstone seeder; a 409 ProblemDetail mapping was out of Step-32 scope | map `DataIntegrityViolationException` → 409 ProblemDetail in demand-account's advice (natural fit: Phase-G hardening or next demand-account touch) | 🔶 open |

**How to add a row:** step · artifact · one-line reason · concrete remediation (or "descope + where recorded") · status (🔶 open / ✅ paid / ✅ descoped).
