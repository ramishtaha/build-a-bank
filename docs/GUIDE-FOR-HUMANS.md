# GUIDE-FOR-HUMANS.md — operating Build-a-Bank with an AI

> You are the human owner of this repo. This guide is for **running and maintaining the course with an AI assistant** (Claude Code or similar) — one step at a time, cheap on context, without quality decay. To *take* the course as a learner, ignore this file and start at the [README](../README.md).

## The 60-second mental model

- The repo contains a **course** (`steps/step-NN/lesson.md`) and the **bank it builds** (`services/`, `gateway/`, `frontend/`…), advancing together under git tags: `step-NN-end` == `step-(NN+1)-start`.
- AI sessions do the building. They are governed by three layers:
  1. **`CLAUDE.md`** (auto-loaded) — non-negotiables + a routing table;
  2. **`docs/ai/`** — the operational layer (authoring contract, context recipe, QA gate, repo map, ledgers);
  3. **`build-a-bank-claude-code-prompt.md`** — the full canonical spec. Only needed for a from-scratch kickoff; day-to-day sessions never read it.
- **`PROGRESS.md`** is the handoff baton: small resume block, last verified tag, explicit next action.

## Generate the next lesson (the main loop)

Open a fresh AI session in the repo and paste:

```
Continue the Build-a-Bank course. Read CLAUDE.md, then follow
docs/ai/CONTEXT-PLAYBOOK.md exactly (its read-set, build order,
and output-budget protocol) to complete the next action in
PROGRESS.md. Verify everything per the Lesson DoD before tagging.
```

That's it — 4 lines instead of the old 663-line paste. The session will: read ~5k tokens of docs → build and verify the code → write the lesson (build section first) → run the QA gate → do the bookkeeping (capsule, PROGRESS, ledger, flashcards, tag).

**A session may not finish a whole step. That's by design.** It stops cleanly at a sub-step boundary and records where. Just open a new session and paste the same prompt.

### What to check after a session (2 minutes)

1. `PROGRESS.md` — did the resume block update? Is "next action" concrete?
2. The lesson's 🔬 Verification Log — real command output present? `Lesson DoD: PASS (…)` line at the top?
3. `git log --oneline -3` and `git tag | tail -3` — commit on `main` + new tag when a step completed.
4. Spot the metrics in `PROGRESS.md` — if 🔮/❓/▶️ counts dive versus recent steps, quality is decaying: say *"run docs/ai/LESSON-CHECKLIST.md against the lesson you just wrote and fix the failures"*.

## Audit a lesson (or all of them)

```
Audit steps/step-12/lesson.md against docs/ai/LESSON-CHECKLIST.md.
Report pass/fail per item with locations. Do not edit anything yet.
```

For a full-course sweep, ask for a multi-agent audit (say "use a workflow") — one auditor per lesson against the same checklist. The 2026-07-02 baseline audit lives in `docs/ai/audit/` (report, per-step findings, and a backlog of what needs a verified re-enrichment session).

## Fix or improve lessons safely

Two categories — keep them separate:

- **Pure-edit fixes** (chunking, predicts, knowledge-checks, time-boxes, session plans, wording): safe for any session. Guardrail to state in your prompt: *"Never invent command output; Verification Logs are read-only."*
- **Anything needing new run output** (missing ▶️/✅ blocks, thin builds): needs a session that **actually runs the code at that step's tag**. Point it at `docs/ai/audit/IMPROVEMENT-BACKLOG.md`:

```
Pick the top item in docs/ai/audit/IMPROVEMENT-BACKLOG.md.
git checkout its step tag on a branch, re-run what the work order
lists, and enrich the lesson with the real captured output.
Follow docs/ai/LESSON-SPEC.md §2. Update the backlog when done.
```

## Keep context small (the rules that keep quality up)

- **Never paste the master prompt** into a working session. If a session asks for more context, it should be reading `docs/ai/PROJECT-MAP.md` or one capsule — not old lessons.
- **Capsules are the memory.** Each step ends with `steps/step-NN/capsule.md` (≤25 lines). If a session re-reads whole prior lessons, stop it and point at the capsule.
- **One step per concern.** Don't ask one session to "finish steps 31–33" — the tag chain is sequential and budget pressure is what caused the last decay.
- **Watch `PROGRESS.md` size.** The resume block stays ≤ ~40 lines; verification detail belongs in `docs/ai/VERIFICATION-LEDGER.md`.

## Change the course conventions

Edit **`docs/ai/LESSON-SPEC.md` and `build-a-bank-claude-code-prompt.md` together** (they're declared in-sync). Never retro-edit 30 lessons by hand for a convention change — record the new rule, apply it forward, and add old lessons to the backlog if the change matters retroactively.

## Troubleshooting the AI loop

| Symptom | Fix |
|---|---|
| Session claims success without pasted output | Quote CLAUDE.md rule #1; ask for the Verification Log; distrust the step until shown |
| Lesson got short/stubby mid-build | The session compressed instead of stopping — revert the lesson, re-run with: "follow CONTEXT-PLAYBOOK §3: stop at a sub-step boundary rather than compress" |
| Session can't find where to resume | `PROGRESS.md` resume block is stale — restore "next action" by checking `git tag | tail` + the last capsule |
| Repo left on detached HEAD | `git switch main` (tags are safe); the convention is commits+tags on `main` |
| Version conflicts on new deps | `VERSIONS.md` is the single source — pin there first, then use |

## Where everything lives

| Thing | Path |
|---|---|
| Course index + progress boxes | `COURSE.md` |
| Resume state (small) | `PROGRESS.md` |
| Per-tag verification history | `docs/ai/VERIFICATION-LEDGER.md` |
| Authoring contract / QA gate / context recipe | `docs/ai/LESSON-SPEC.md` · `LESSON-CHECKLIST.md` · `CONTEXT-PLAYBOOK.md` |
| Repo fact map for callbacks | `docs/ai/PROJECT-MAP.md` |
| Skipped-artifact register | `docs/ai/CONTRACT-DEBT.md` |
| Audit baseline (2026-07-02) | `docs/ai/audit/` |
| Canonical spec (kickoff only) | `build-a-bank-claude-code-prompt.md` |
