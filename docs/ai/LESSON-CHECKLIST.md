# LESSON-CHECKLIST.md — the Lesson Definition of Done as a runnable QA gate

> Run this against ONE lesson (`steps/step-NN/lesson.md`) after authoring, or when auditing.
> Every item is pass/fail. Record the result line in the lesson's 🔬 Verification Log:
> `Lesson DoD: PASS (N sub-steps · 🔮 X · ❓ Y · 🔬 Z · ▶️ W)` — or stop and fix / resume next session.
> Greppable checks include the command; judgment checks state what to look for.

## A. Structure (greppable)

- [ ] **Six movement headers present** — `grep -c "^# [A-F] ·" lesson.md` → 6
- [ ] **Spine sections present** — grep each: `📋`, `⏭️`, `📇`, `🎯`, `✅ What You`, `🧰`, `🗓️ Session Plan`, `🧠 The Big Idea`, `🌱`, `📦`, `🛠️`, `🎮`, `🏁`, `🔬`, `💼`, `🏋️`, `🏆`, `🃏`
- [ ] **Mini-TOC anchors resolve** — every `](#x)` in the 🧭 table has a matching `<a id="x">` or heading slug
- [ ] **Verification tier stated** — Verification Log names 🟢/🟠/🔴
- [ ] **Flashcards: 3–5** Q/A pairs, appended to `docs/flashcards.md`
- [ ] **`Depends on: Steps` line present** in 🧰
- [ ] **Capsule exists** — `steps/step-NN/capsule.md`, ≤25 lines, has all 5 blocks (exists / added / gotchas / hooks / next)

## B. The sacred build (counts + judgment)

- [ ] **Build ≥ half the lesson body** (line count from `# C ·` to `# D ·` vs total)
- [ ] **Every sub-step has full micro-anatomy** — for each `Sub-step` heading, the fields 🎯 📁 ⌨️ 🔍 💭 🔮 ▶️ ✋ 💾 ⚠️ appear before the next sub-step heading (spot-check all, not a sample)
- [ ] **Every ⌨️ code block**: file-path header comment, language tag, complete imports, no `...`
- [ ] **Every sub-step has ▶️ + ✅ expected output** — and the output is from a real run (cross-check tone/ports against the Verification Log; random Testcontainers ports beat suspicious fixed ones)
- [ ] **Interactivity minimums** — `grep -c 🔮` ≥ 3 · `grep -c ❓` ≥ 3 · **≥1 break-it**: `grep -c "🔬 \*\*Break"` ≥ 1 (don't count bare 🔬 — the mini-TOC, the `# D ·` movement header, and the DoD line all carry 🔬 without being break-its) · 🧭 you-are-here on sub-step headings when >3 sub-steps
- [ ] **Opens with 🗺️ Mermaid + 🌳 file tree; closes with 🔁 sequence diagram**
- [ ] **First win ≤10 min** — first sub-step ends in something run-and-seen
- [ ] **Scaffold fades** — at least one later sub-step is ⌨️ type-it-yourself with the solution in `<details>`

## C. ADHD / flow (judgment, fast to check)

- [ ] **Session Plan**: 6–10 sittings, ~2–3 h each, each ends at a named ✋ checkpoint; optional routes listed with time costs
- [ ] **Re-entry ritual at every ✋** — "Stopping here? … Next session: … first action: …"
- [ ] **Time estimates** on every movement and sub-step; every 🚀/optional block labeled `+~N min`
- [ ] **No walls of text** — scan for 150+-word stretches without a visual/code/interaction break
- [ ] **Micro-recaps** — distant cross-references restate the essential line
- [ ] **≤ ~3 new terms per sub-step**; jargon defined at first use

## D. Pedagogy (judgment)

- [ ] **Skip-test is performance-based** — do-tasks with pass criteria, not "do you know…?"
- [ ] **Every ✅ outcome maps to ≥1** ✋ / ❓ / 🏋️ / 🧠 Test-Yourself item
- [ ] **Test Yourself + flashcards test the core** (not trivia); answers hidden in `<details>`
- [ ] **🔗 How This Connects** calls back ≥1 earlier step and teases the next
- [ ] **Interview prep**: 4–6 Qs; version-evolution Q if 🕰️ exists; concurrency Q if shared state
- [ ] **🩺 includes *(anticipated)* learner errors**, not only errors actually hit
- [ ] **One near-transfer exercise** in 🏋️

## E. Engineering honesty (hard gates)

- [ ] **No invented output** — every ✅ block traceable to a real run (the Verification Log is the anchor)
- [ ] **No weakened/disabled tests**; mutation sanity-check present at 🔴 tier and on money/security/concurrency paths
- [ ] **Verify-adjacent items say so prominently** (per `CAPABILITIES.md`)
- [ ] **Version numbers referenced, not restated** (single source: `VERSIONS.md`)
- [ ] **Money = BigDecimal, time = UTC Instant** in every snippet
- [ ] **Bookkeeping done** — PROGRESS.md resume block + lesson metrics · VERIFICATION-LEDGER row · CONTRACT-DEBT entries for anything promised-but-skipped · COURSE.md box ticked

## Scoring shortcut for audits

Count failures by section: A/B fails = structural (fix now or the step is not done) · C/D fails = quality debt (fix in an edit pass) · E fails = **stop the line** — §12 territory.
