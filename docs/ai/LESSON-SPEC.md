# LESSON-SPEC.md — the per-step authoring contract (operational distillation)

> Distilled from `build-a-bank-claude-code-prompt.md` Parts V–VI (kept in sync — change both or neither).
> Audience: an AI session authoring **one** lesson. Read this + `CONTEXT-PLAYBOOK.md`; do not read the master prompt.
> Last synced: 2026-07-02.

## 0. The one-sentence contract

Write for a nervous beginner following along at 11pm with no one to ask — who must end the night with working software they *saw run* — while keeping an experienced engineer engaged via density, depth, and skippable fast-tracks.

## 1. The six movements — every lesson, this shape

Open with a `Step N of 67 · Phase X 🔵` banner + a 🧭 one-line mini-TOC of the six movements (clickable anchors).

**★ = spine (never omit) · ◇ = only when it genuinely adds value**

| # | Section | Notes |
|---|---|---|
| **A · 🧭 Orient** | | |
| 1★ | 📋 This Step in 30 Seconds | title, step #, badge, effort estimate, **what to run** (minimal services/infra). Ends with **⏭️ Can You Skip This Step?** — a 5-min **performance-based** check: 2–3 do-tasks with concrete pass criteria ("write X; it should print Y"), never "do you feel you know X?" |
| 2★ | 📇 Cheat Card | one-screen TL;DR: key commands, the one headline diagram/snippet, one sentence of what this step delivers |
| 3★ | 🎯 Why This Matters | 2–3 sentence hook (real systems, interviews, paycheck) |
| 4★ | ✅ What You'll Be Able to Do | concrete outcomes, **each mapped to ≥1 ✋/❓/🏋️/🧠 Test-Yourself item** (constructive alignment) |
| 5★ | 🧰 Before You Start | prerequisites + callback to earlier learning + `Depends on: Steps X, Y` line |
| 6★ | 🗓️ Session Plan | **6–10 named sittings (~2–3 h each), each ending at a ✋ checkpoint.** Present optional routes (skip-test / fast-track / 🚀 asides) as a menu with time costs. Stopping mid-step = planned success |
| **B · 🧠 Understand** | | |
| 7★ | 🧠 The Big Idea | the why + theory, with **a diagram and an analogy**; dense, no filler |
| 8◇ | 🧩 Pattern Spotlight | problem → why-it-fits → alternatives/trade-offs → implementation |
| 9★ | 🌱 Under the Hood | how the Spring/JVM/DB feature *actually* works — nothing is magic |
| 10◇ | 🛡️ Security Lens | what could go wrong (even in non-security steps) |
| 11◇ | 🕰️ Then vs. Now | old → new → why → what legacy still uses (only for real version deltas) |
| **C · 🛠️ Build** | | |
| 12★ | 📦 Your Starting Point | tag `step-NN-start`; what's green vs. what you'll build |
| 13★ | 🛠️ Let's Build It | **the heart — see §2.** Longest, most detailed section |
| 14★ | 🎮 Play With It | `requests.http` + curl, `make` helpers, exactly what to try + what you'll see, 🧪 little experiments list |
| 15★ | 🏁 The Finished Result | tag `step-NN-end` + learner-facing ✅ Definition-of-Done checklist |
| **D · 🔬 Prove** | | |
| 16★ | 🔬 Prove It Works | the Verification Log: stated tier (🟢/🟠/🔴) + **real pasted output** incl. `smoke.sh`; opens with the Lesson-DoD line (§4) |
| **E · 🎓 Apply** | | |
| 17◇ | 🚀 Go Deeper | `<details>` asides, **each labeled "+~N min"**; outside the effort budget |
| 18★ | 💼 Interview Prep | 4–6 Q&A in `<details>`; a version-evolution Q where one exists; a concurrency Q wherever shared state is involved |
| 19★ | 🏋️ Your Turn | quick exercises (answers in `<details>`) + stretch goals; **one near-transfer exercise** (same pattern, different service); promised-but-unshipped solutions → `CONTRACT-DEBT.md` |
| **F · 🏆 Review** | | |
| 20◇ | 🩺 Troubleshooting | real errors hit (verbatim → cause → fix) **+ anticipated learner errors** labeled *(anticipated)*; reset instructions; `make doctor` |
| 21◇ | 📚 Resources & Glossary | curated links + this step's terms |
| 22★ | 🏆 Recap & Study Notes | (a) key points (b) Key Terms (c) 🧠 Test Yourself 3–5 Qs in `<details>` (d) 🔗 How This Connects (e) résumé line (f) ✅ "You can now…" (g) 🃏 3–5 flashcards, appended to `docs/flashcards.md`, + "🔁 revisit in ~N steps" (h) ✍️ one-line reflection (i) motivating sign-off |
| 23★ | 🧳 Context Capsule | write `steps/step-NN/capsule.md` — see §5 |

Extras: 🧵 thread-safety note (from Step 12 on, wherever shared mutable state appears, pointing to Step 11). 🧠 **Cumulative Review at steps 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65** — mixed quiz interleaving ~30% older material.

## 2. The 🛠️ build — the sacred section

Open with 🗺️ what-we'll-build Mermaid diagram + 🌳 files-we'll-touch tree. Close with a full end-to-end ▶️ run + 🔁 sequence diagram of the flow just built. In between: **many small numbered sub-steps**, each with the full micro-anatomy, **in this order**:

> 🎯 **Goal** (1–2 lines) → 📁 **Exact location** (full path; new file or edit) → ⌨️ **Code** (complete; file-path header comment; all imports; compiles as shown; before→after diff for edits) → 🔍 **Line-by-line** (every annotation/import/config key; jargon defined inline) → 💭 **Under the hood** → 🔮 **Predict** → ▶️ **Run & See** (exact command + fenced ✅ expected output + ❌ common-wrong-output) → ✋ **Checkpoint** → 💾 **Commit** (conventional message) → ⚠️ **Pitfall**

**Non-negotiables:**
- Complete, runnable code — never fragments, never `...`, never "add the usual imports".
- One idea per sub-step; run between pieces; ≤ ~3 new terms per sub-step (overflow → 🚀).
- Explain every new token the first time it appears — including `mvn`/`git`/`docker` subcommands.
- **Every sub-step has a ▶️ Run & See with real output.** Expected-output blocks come from actual runs — never invented (§12 protocol).
- **First win ≤10 min** — the first sub-step of the step *and of each sitting* ends with something that visibly runs.
- **Every sub-step carries a ~time estimate**; every movement too.
- ✋ checkpoints are frequent, and each carries a **re-entry ritual**: *"Stopping here? You have ⟨X working⟩. Next session: sub-step ⟨N⟩; first action: ⟨exact file/command⟩."*
- **Scaffold fades within the build:** early sub-steps fully worked; later ones shift toward ⌨️ type-it-yourself (full solution still in `<details>`).
- **No walls of text:** never ~150+ words without a visual, code block, or interaction.
- **Micro-recap on distant references:** restate the one essential line instead of forcing a scroll-back.
- Editor-agnostic core path (CLI + any editor); IntelliJ only as optional "💡 Faster in IntelliJ" asides *after* the neutral instructions.
- Interactivity toolkit, sprinkled: 🔮 predict-then-run · ⌨️ type-it-yourself · 🔬 break-it-on-purpose · ❓ knowledge-check (answer in `<details>`) · 🧭 "you are here" markers.
- Commit after each logical unit; inline mini-troubleshooting where errors are likely.

**The bar:** match the depth and shape of the gold-standard worked example — see `steps/step-12/lesson.md` sub-steps 1–3 (entities → repository → service) for an in-repo exemplar, or master prompt §8.1 for the canonical one.

## 3. Artifacts per step

- **Mandatory:** `lesson.md` · `capsule.md` · `requests.http` (+ curl equivalents, when endpoints exist) · `smoke.sh` · Makefile helpers where promised in the lesson.
- **Optional (skip freely; log in `CONTRACT-DEBT.md` if promised then skipped):** Bruno/Postman collections · seed data + `make seed-NN` · `solutions/step-NN/` folders · `concepts/` docs.

## 4. Lesson Definition of Done — the document gate

Record at the top of the Verification Log: `Lesson DoD: PASS (N sub-steps · 🔮 X · ❓ Y · 🔬 Z · ▶️ W)`.

1. Spine complete (every ★ above, greppable).
2. 🗓️ Session Plan present (6–10 sittings, each ending at a ✋).
3. Sacred build intact — build ≥ half the lesson body; **every** sub-step has the full micro-anatomy incl. real ▶️ + ✅.
4. Interactivity minimums — 🔮 ≥3 · ❓ ≥3 · 🔬 ≥1 · 🧭 markers if >3 sub-steps.
5. First win ≤10 min (per step and per sitting).
6. Time-boxes on every movement + sub-step; "+~N min" on everything optional.
7. Re-entry ritual at every ✋.
8. Readability: no 150+-word walls; micro-recaps; ≤3 new terms/sub-step.
9. Every ✅ outcome maps to ≥1 assessment item.
10. Recap complete (a–i); flashcards appended; capsule written.
11. Mini-TOC + internal anchors resolve.
12. Lesson metrics in `PROGRESS.md`; skipped artifacts in `CONTRACT-DEBT.md`.

**If any criterion would fail for length/budget reasons: stop at a sub-step boundary and resume next session. Forbidden compressions (§12-level violations): stub sub-steps · dropped ▶️/✅ blocks · "the rest is similar" on the core path · skipped 🔍 line-by-line on new code · omitted Session Plan/checkpoints/re-entry lines.**

## 5. The 🧳 Context Capsule (`steps/step-NN/capsule.md`)

≤25 lines, no narrative — the only thing the next session reads about this step:
- **What now exists** — modules, key endpoints + ports, test counts, the `step-NN-end` tag.
- **What this step added/changed** — one line each.
- **Gotchas discovered** — workarounds, version quirks that will bite next session.
- **Callback hooks** — the 2–3 facts future lessons will reference.
- **Next step's starting expectation** — tag, what's green, what's promised next.

## 6. Code verification (summary — full protocol: master prompt §12)

- Tiers: 🔴 Full (milestones, money/security/concurrency, new services — incl. mutation sanity-check §12.3 + clean-room §12.4) · 🟠 Standard (most feature steps — `./mvnw verify` + tests + real run output + `smoke.sh`) · 🟢 Light (docs/config — build + affected tests). State the tier in every Verification Log.
- Paste hard-to-fake artifacts: Testcontainers random high ports, Flyway lines, HTTP status + body, failing-then-fixed concurrency runs.
- Mutation sanity-check on critical paths: break code → show test FAIL → revert → show pass.
- Never game the gate: no `@Disabled`, no weakened assertions, no known-flaky "done".
- Honesty about what can't run here (see `CAPABILITIES.md`): verify-adjacent + exact commands + expected output for the learner, stated prominently.

## 7. Voice & graphics

- Warm, growth-mindset, dense, never patronizing; bugs are normal. Frame ideas the way interviewers probe them.
- Dual coding: a diagram for every concept-heavy idea — Mermaid (architecture, sequence, ER, state, flow), ASCII sketches, comparison tables, before/after snippets.
- Emoji are signposts paired with words, never meaning alone (accessibility); alt-text for every diagram; language-tagged fences always; `<details>` for solutions/asides/answers.
- ADHD-aware is the house style, not an accommodation: session plans, time-boxes, first-win-fast, re-entry rituals, no walls of text, labeled costs, choice menus.
- Money = `BigDecimal` (minor units, rounding stated) · time = UTC `Instant` — in every snippet.
- Version facts live **only** in `VERSIONS.md` — reference, don't restate.

## 8. Iconography quick key

📋 30-sec · 📇 cheat · 🎯 why · ✅ outcomes · 🧰 before · 🗓️ session plan · 🧠 big idea / test-yourself · 🧩 pattern · 🌱 internals · 🛡️ security · 🕰️ versions · 🧵 threads · 📦 start · 🛠️ build · 🎮 play · 🏁 end · 🔬 prove/break-it · 🚀 deeper · 💼 interview · 🏋️ practice · 🩺 troubleshoot · 📚 resources · 🏆 recap · 🃏 flashcards · 🧳 capsule · 🎓 capstone · 🔮 predict · ⌨️ type-it · ❓ check · ✋ checkpoint · 💾 commit · ⚠️ pitfall · 🧭 you-are-here · ✅/❌ right/wrong output · 💡 IDE aside
