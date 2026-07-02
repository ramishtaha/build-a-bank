# CLAUDE.md — Build-a-Bank (read this, then route)

**What this repo is:** a 67-step self-teaching course + the educational banking platform it builds (Java 25 · Spring Boot 4 · Maven multi-module · React/TS frontend · Postgres/Redis/Redpanda via Testcontainers). Steps live in `steps/step-NN/lesson.md`; code is cumulative; every step is git-tagged (`step-NN-end` == `step-(NN+1)-start`). Canonical spec: `build-a-bank-claude-code-prompt.md` (35k tokens — do **not** read it for routine work; the distilled layer below replaces it).

## Route by task — load only what the task needs

| Task | Read (in order) | Do NOT read |
|---|---|---|
| **Generate the next lesson/step** | `docs/ai/CONTEXT-PLAYBOOK.md` → it lists the exact ~5k-token read-set | the master prompt; prior lessons wholesale; `docs/ai/VERIFICATION-LEDGER.md` |
| **Resume interrupted work** | `PROGRESS.md` (resume block) → follow its "Next action" | anything else until PROGRESS says so |
| **Audit / review a lesson** | `docs/ai/LESSON-CHECKLIST.md` + the one lesson file | other lessons |
| **Enrich a lesson with new run output** | `docs/ai/audit/IMPROVEMENT-BACKLOG.md` (top work order) + `docs/ai/LESSON-SPEC.md` §2 — run the code at that step's tag | inventing output; other lessons |
| **Author/edit lesson content** | `docs/ai/LESSON-SPEC.md` (the per-step contract) | the master prompt |
| **Answer "what exists where?"** | `docs/ai/PROJECT-MAP.md` (modules, ports, patterns, introducing steps) | grepping all of `services/` |
| **Check what this sandbox can run** | `CAPABILITIES.md` | — |
| **Check/pin a version** | `VERSIONS.md` — the ONLY place version numbers live | — |
| **Course structure / what's in step N** | the step-N row of `COURSE.md` | the whole COURSE.md |
| **Human operator instructions** | `docs/GUIDE-FOR-HUMANS.md` | — |

## Non-negotiables (full detail: master prompt §12, §8; distilled in docs/ai/)

1. **Verify, don't claim.** Every "it works" is backed by real pasted command output. Never fabricate, summarize-as-if-run, or invent expected output. If you didn't run it, you don't claim it.
2. **The chain holds.** `step-NN-end` == `step-(NN+1)-start`; both build clean (`./mvnw verify`). Steps are strictly sequential.
3. **The 🛠️ build is sacred.** Never thin a lesson's hands-on build to save space/time. Out of budget → finish the current sub-step, update `PROGRESS.md`, **stop**. A thin build is a broken build.
4. **The lesson document is gated** by the Lesson Definition of Done (`docs/ai/LESSON-CHECKLIST.md`) — same status as BUILD SUCCESS.
5. **No real secrets, ever.** Fake/demo credentials only. Educational, non-production project.
6. **Pinned versions only** — never `latest`; `VERSIONS.md` is the single source of truth.
7. **Autonomous + honest.** Take the senior choice, record it (ADR if architectural); state plainly what you couldn't run (verify-adjacent per `CAPABILITIES.md`).
8. **Bookkeeping after every step:** Lesson DoD → `steps/step-NN/capsule.md` → `PROGRESS.md` resume block + lesson metrics → row in `docs/ai/VERIFICATION-LEDGER.md` → tag. Skipped promised artifacts go in `docs/ai/CONTRACT-DEBT.md`, never silently dropped.

## Repo map (one line each)

```
steps/step-NN/          lesson.md + capsule.md + requests.http + smoke.sh   ← the course
services/               cif · demand-account · auth · notification · market-info · onboarding (Spring Boot modules)
gateway/  frontend/     Spring Cloud Gateway (front door) · React 19 + TS + Vite SPA
playground/             concurrency-lab · distributed-lab · jvm/net/java-basics labs (pure-JVM teaching modules)
libs/common/            the custom Spring Boot starter (Step 28)
docs/ai/                AI operational layer: LESSON-SPEC · CONTEXT-PLAYBOOK · LESSON-CHECKLIST · PROJECT-MAP · VERIFICATION-LEDGER · CONTRACT-DEBT · audit/
adr/  security/         Architecture Decision Records · threat model + risk register
COURSE.md  PROGRESS.md  curriculum index + progress tracker · resume state (small; ledger moved to docs/ai/)
Makefile                doctor · verify · run-<svc> · play-NN helpers
```

## House rules for working here

- Build/test: `./mvnw verify` (full) or `./mvnw -pl services/<svc> -am verify` (one module). Docker required for Testcontainers-backed tests.
- Commits: conventional messages (`feat(cif): …`); commits land on `main`, tags on `main` — don't leave the repo detached.
- Lessons are learner-facing prose — warm, dense, no filler; code blocks always carry a file-path header comment and complete imports.
- Money = `BigDecimal`, time = UTC `Instant` — everywhere, including lesson snippets.
- When the per-step contract itself changes: update **both** the master prompt and `docs/ai/LESSON-SPEC.md` (they are kept in sync; the spec states this).
