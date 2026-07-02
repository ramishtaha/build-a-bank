# CONTEXT-PLAYBOOK.md — generate step N with minimal context

> The recipe an AI session follows to produce the next step **one step at a time** with a ~5k-token doc read-set instead of 40k+. Deviating from this read-set is how lesson quality decayed once already (steps 19–30 thinned as sessions burned context) — follow it.

## 1. The read-set (exact, in order — and nothing else)

| # | Read | Why | ~Tokens |
|---|---|---|---|
| 1 | `PROGRESS.md` (whole file — it's ≤ ~40 lines) | where we are, last verified tag, **next action** | 400 |
| 2 | `steps/step-<N-1>/capsule.md` | what exists + gotchas, instead of the prior 1,000-line lesson | 300 |
| 3 | The **step-N row** of `COURSE.md` (grep for `| N |`) + its phase intro paragraph | scope + focus of the step you're building | 200 |
| 4 | `CAPABILITIES.md` | what this sandbox can actually run | 700 |
| 5 | `VERSIONS.md` | the pinned set (never restate numbers in lessons) | 900 |
| 6 | `docs/ai/LESSON-SPEC.md` | the authoring contract | 2,500 |
| 7 | `docs/ai/PROJECT-MAP.md` | accurate callbacks without re-reading lessons | 800 |

**Do NOT read:** the master prompt (`build-a-bank-claude-code-prompt.md`) · prior lessons wholesale · `docs/ai/VERIFICATION-LEDGER.md` · `docs/flashcards.md` (append-only) · `node_modules`, `target/`, logs.

**Targeted exceptions (pull the minimum):**
- The new step **directly extends existing code** → read only the specific files you'll modify (the capsule names them).
- A callback needs a precise detail PROJECT-MAP lacks → grep the specific earlier lesson **section**, don't open the whole file.
- If `steps/step-<N-1>/capsule.md` doesn't exist (older steps predate capsules) → read the prior lesson's 🏆 Recap + 🏁 Finished Result sections only, then **write the missing capsule** as part of your step (backfill, ≤25 lines).

## 2. The build order (code first, prose second)

1. **Re-verify the ground:** you're on `main` (never detached); previous `step-NN-end` builds (`./mvnw verify` or the affected module).
2. **Build + verify the code** for step N completely — tests, smoke.sh, real outputs captured as you go. The lesson quotes these runs; you cannot write ▶️/✅ blocks before the runs exist.
3. **Write the 🛠️ build section first** (it's ~half the lesson and quotes your captured outputs), then the rest of the movements around it.
4. **Run the Lesson DoD** (`docs/ai/LESSON-CHECKLIST.md`) — fix or stop, never compress.
5. **Bookkeeping (all of it, every step):**
   - write `steps/step-NN/capsule.md` (≤25 lines, spec §5)
   - update `PROGRESS.md` resume block: step done, tag, next action, **lesson metrics** (lines · sub-steps · 🔮/❓/🔬/▶️ counts)
   - append the verification row to `docs/ai/VERIFICATION-LEDGER.md`
   - append 3–5 flashcards to `docs/flashcards.md`
   - log any skipped promised artifact in `docs/ai/CONTRACT-DEBT.md`
   - tick the step's box in `COURSE.md`
   - commit on `main`, tag `step-NN-end`

## 3. Output-budget protocol (the anti-decay rule)

A full lesson is large (a healthy build-heavy lesson runs 900–2,000 lines). When context/output pressure hits:

- **Finish the current sub-step cleanly (full micro-anatomy) → commit → update `PROGRESS.md` ("stopped after sub-step K of step N; next: sub-step K+1") → stop.**
- The next session resumes from that sub-step with this same playbook.
- **Never** compress micro-anatomy, stub sub-steps, drop ▶️/✅ blocks, or write "the rest is similar". Stopping early always beats compressing. The lesson metrics in `PROGRESS.md` make thinning visible; the Lesson DoD blocks the step.
- If one file gets unwieldy, split `lesson.md` + `build.md` at a movement boundary.

## 4. Session kickoff prompt (what a human pastes — also in docs/GUIDE-FOR-HUMANS.md)

```
Continue the Build-a-Bank course. Read CLAUDE.md, then follow
docs/ai/CONTEXT-PLAYBOOK.md exactly (its read-set, build order,
and output-budget protocol) to complete the next action in
PROGRESS.md. Verify everything per the Lesson DoD before tagging.
```

That's the whole prompt. Everything else is routed from the repo.

## 5. Guardrails that always apply

- Real output only — never invent a `✅ Expected output` block (§12 protocol; the repo's prime directive).
- `step-NN-end` == `step-(N+1)-start`, both green, or the step isn't done.
- No real secrets; fake/demo credentials only.
- New architectural decision → ADR in `adr/`.
- Contract change (movements, DoD, anatomy) → update `docs/ai/LESSON-SPEC.md` **and** the master prompt together.
