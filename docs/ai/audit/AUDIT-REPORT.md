# Build-a-Bank Lesson Audit — Synthesis Report (Steps 1–30)

> Synthesized 2026-07-02 from `docs/ai/audit/findings/step-01.md … step-30.md`, `cross-consistency.md`, and `cross-metadocs.md`.
> Companion file: `IMPROVEMENT-BACKLOG.md` (work orders for everything that needs re-running, not just editing).

> [!NOTE]
> **Post-fix addendum (same day).** The scorecard below is the **pre-fix baseline**. A pure-edit improvement pass then applied **366 fixes across all 30 lessons** (+11,095 / −1,292 lines): 🗓️ Session Plans + per-movement/sub-step time-boxes + ✋ re-entry rituals everywhere; real code transcribed from each step's git tag into the fragment/stub builds (verbatim, `diff`-verified — no invented output anywhere); sequencing errors (T7) reordered; dead `solutions/` references rerouted; era-3 spine items restored; all 30 🧳 capsules backfilled; meta-docs honesty-synced. Every lesson now meets 🔮 ≥3 / ❓ ≥3 (measured post-fix in `PROGRESS.md` → Lesson metrics). **Still open** (needs real runs — see `IMPROVEMENT-BACKLOG.md`): per-sub-step ▶️ Run & See + ✅ output capture for steps 18–30, first-win feedback loops, Verification-Log evidence re-runs (T6), and type-it-yourself fading on thin steps.

---

## 1. Executive summary

The course is a tale of two halves. Steps 1–9 are close to the contract's ideal: complete micro-anatomy, verified code, real pasted output, dense interactivity, honest failure-driven teaching. From Step 10 the build movement begins to hollow out — first excerpts ("full file in the repo"), then fragments without imports, and from Step 19 onward **zero code in the build at all**: 19 of 30 lessons are flagged `thinBuild:true`. The decay is measurable: lesson length falls from 1,200–1,985 lines (Steps 1–10) to 304–493 (Steps 18–30); checkpoints hit **zero for eleven consecutive lessons (20–30)**; predicts collapse from 5–12 per lesson to one. Crucially, the *working code exists and is tag-verified in the repo* for every thin step — the lessons describe real systems they never show, so the fix is transcription plus honest re-runs, not new development.

Total: **333 findings** (95 high / 161 medium / 77 low), of which **80 require re-running code** (`needsRun:true`) and the rest are pure edits.

**Top 5 systemic problems:**
1. **Thin builds (Steps 10–30, 19 lessons):** the "sacred build" contract — a nervous beginner completes the step unaided — is broken for two-thirds of the course, worst on money/security/concurrency steps (11, 12, 14, 16, 17, 21, 24).
2. **Zero ADHD session architecture in all 30 lessons:** 8–22-hour steps ship with no sitting plans, no per-movement/sub-step time-boxes, and no re-entry lines at checkpoints.
3. **Evidence-integrity gaps:** ~16 lessons claim "real pasted output" for Verification Log or run-and-see blocks that are paraphrased, hand-edited, wrong (`-q` silences output; impossible Maven line order), or absent.
4. **Interactivity decay:** type-it-yourself appears in **zero** lessons; knowledge-checks, break-its, you-are-here markers, and diagrams all decay to near-zero after Step 17.
5. **Broken sequencing and dead references:** commands that fail at the point they're given (5, 13, 14), checkpoints demanding tests never shown (17, 21, 23, 30), `solutions/step-NN/` pointers where only `step-01` exists (~29 lessons), and stale/contradictory meta-docs.

**Genuinely strong, everywhere:** the Verification Log discipline (stated tiers, §12.3 mutation checks with real failure output, §12.8 honesty notes) survives even in the thinnest lessons; the Understand movements are technically accurate, current (Boot 4 / Java 25 / Spring Security 7 deltas taught explicitly), and interview-sharp; misconception handling and cross-step continuity are exceptional; and Steps 1–9 are a working template of what the whole course should look like.

---

## 2. Per-step scorecard

Scores are 1–10 per lens. Finding counts are high/medium/low severities.

| Step | swe | pedagogy | adhd | structure | thinBuild | High | Med | Low | Total |
|---|---|---|---|---|---|---|---|---|---|
| 01 | 8 | 6 | 5 | 8 | no | 3 | 5 | 4 | 12 |
| 02 | 8 | 8 | 5 | 9 | no | 2 | 5 | 4 | 11 |
| 03 | 8 | 8 | 6 | 9 | no | 1 | 5 | 4 | 10 |
| 04 | 8 | 8 | 6 | 9 | no | 2 | 5 | 3 | 10 |
| 05 | 8 | 8 | 6 | 8 | no | 3 | 5 | 2 | 10 |
| 06 | 7 | 8 | 5 | 9 | no | 3 | 5 | 2 | 10 |
| 07 | 9 | 8 | 6 | 9 | no | 1 | 6 | 3 | 10 |
| **A avg (1–7)** | **8.0** | **7.7** | **5.6** | **8.7** | 0/7 | | | | |
| 08 | 8 | 8 | 5 | 8 | no | 2 | 7 | 3 | 12 |
| 09 | 8 | 8 | 6 | 8 | no | 2 | 4 | 5 | 11 |
| 10 | 7 | 8 | 5 | 8 | no | 3 | 5 | 2 | 10 |
| 11 | 5 | 7 | 4 | 7 | **yes** | 3 | 6 | 2 | 11 |
| 12 | 6 | 7 | 4 | 7 | **yes** | 3 | 7 | 1 | 11 |
| **B avg (8–12)** | **6.8** | **7.6** | **4.8** | **7.6** | 2/5 | | | | |
| 13 | 7 | 7 | 4 | 8 | no | 4 | 6 | 2 | 12 |
| 14 | 6 | 7 | 4 | 8 | **yes** | 4 | 6 | 2 | 12 |
| 15 | 7 | 6 | 4 | 8 | **yes** | 3 | 4 | 3 | 10 |
| 16 | 7 | 6 | 4 | 8 | **yes** | 4 | 6 | 2 | 12 |
| 17 | 6 | 6 | 4 | 7 | **yes** | 3 | 7 | 2 | 12 |
| 18 | 7 | 6 | 4 | 6 | **yes** | 5 | 5 | 2 | 12 |
| **C avg (13–18)** | **6.7** | **6.3** | **4.0** | **7.5** | 5/6 | | | | |
| 19 | 6 | 5 | 4 | 5 | **yes** | 4 | 5 | 3 | 12 |
| 20 | 6 | 4 | 3 | 5 | **yes** | 3 | 5 | 3 | 11 |
| 21 | 5 | 4 | 3 | 5 | **yes** | 3 | 6 | 1 | 10 |
| 22 | 5 | 4 | 3 | 4 | **yes** | 4 | 6 | 2 | 12 |
| 23 | 4 | 3 | 2 | 5 | **yes** | 4 | 5 | 3 | 12 |
| 24 | 5 | 4 | 3 | 5 | **yes** | 4 | 6 | 2 | 12 |
| **D avg (19–24)** | **5.2** | **4.0** | **3.0** | **4.8** | 6/6 | | | | |
| 25 | 6 | 4 | 3 | 4 | **yes** | 3 | 4 | 2 | 9 |
| 26 | 5 | 4 | 2 | 5 | **yes** | 4 | 5 | 3 | 12 |
| 27 | 7 | 5 | 4 | 6 | **yes** | 4 | 5 | 1 | 10 |
| 28 | 6 | 4 | 3 | 5 | **yes** | 3 | 6 | 3 | 12 |
| **E avg (25–28)** | **6.0** | **4.3** | **3.0** | **5.0** | 4/4 | | | | |
| 29 | 5 | 3 | 3 | 5 | **yes** | 4 | 5 | 3 | 12 |
| 30 | 6 | 4 | 3 | 5 | **yes** | 4 | 4 | 3 | 11 |
| **F avg (29–30)** | **5.5** | **3.5** | **3.0** | **5.0** | 2/2 | | | | |
| **Course total** | | | | | **19/30** | **95** | **161** | **77** | **333** |

The decay curve is unambiguous: every lens degrades monotonically from Phase A→D, with a small Phase E recovery on swe/structure (Steps 25–28 are repo-accurate even though the builds are stubs) and pedagogy/ADHD staying at the floor through Phase F. ADHD is the weakest lens in *every* phase — even Phase A never exceeds 6.

---

## 3. Systemic themes

Each theme appears in many lessons; the per-step files carry the exact locations. Deduplicated to 15.

### T1 — Thin builds: the sacred build hollows out after Step 10
**Steps:** 10 (excerpts) → 11–18 (fragments without packages/imports) → 19–30 (zero code). 19 lessons flagged.
**SWE:** the build cannot be completed from the lesson; checkpoints and DoDs demand test counts for tests never shown.
**Pedagogy:** every claim in Prove references unexplained magic.
**Fix:** verified re-enrichment (see backlog): transcribe the real, tag-verified repo files into full micro-anatomy, and capture real per-sub-step run output. This is transcription + runs, not new development.

### T2 — No ADHD session architecture, anywhere
**Steps:** all 30. Effort figures of 8–22h with no sitting plans, no named save points, no per-movement or per-sub-step time-boxes, no re-entry lines ("stopping here? you have X; next session: sub-step N, first action …").
**ADHD:** the single biggest abandonment risk in otherwise-strong lessons; time-blindness has no scaffold.
**Fix:** one template block per lesson (sitting table + time-tagged headings + one-line re-entry notes at checkpoints). Pure edit.

### T3 — Run-and-see coverage collapses; common-wrong-output is near-universally absent
**Steps:** partial in 5, 8, 9, 11–17; absent entirely in 18–30. Common-wrong-output missing in ~25 lessons.
**SWE:** no way to confirm intermediate state; failures surface far from their cause.
**Fix:** per-sub-step command + genuinely captured output + one induced failure signature. Mostly needsRun.

### T4 — Reward-loop starvation / late first win
**Steps:** 2, 5, 8, 12–14, 16, 18, 20–22, 25–30. Hours (up to 10+) of typing with compile-only or zero feedback; several lessons' first visible run is after the entire build.
**ADHD:** violates first-win-fast; **pedagogy:** no formative feedback.
**Fix:** a ~10-minute first win at the top of every build + something visible per sitting (jshell interludes, early boots, targeted test slices). Mostly needsRun for truthful output.

### T5 — Zero scaffold fading: no type-it-yourself in any of 30 builds
**Steps:** 1–17 are 100% fully-worked copy-paste; 18–30 have nothing to fade.
**Pedagogy:** learners never produce code from memory before end-of-step exercises; DoD "explain in your own words" is unearned.
**Fix:** convert 1–2 late, low-risk artifacts per lesson to spec-plus-hidden-solution. Pure edit for 1–17; depends on T1 for thin steps.

### T6 — Verification Log evidence gaps under a "real pasted output" banner
**Steps:** 1, 2, 6–12, 14, 17–19, 25, 26, 29 (paraphrased summaries, hand-edited fences, `…` elisions with inline annotations, prose-only clean-room/smoke claims).
**SWE:** undermines the course's hardest-won asset — its honesty brand.
**Fix:** re-run the named commands at the step tag and paste raw tails; move commentary outside fences. needsRun.

### T7 — Broken sequencing: commands that fail exactly where they're given
**Steps:** 5 (run before application.yml exists; `java -jar` before any `package`), 13 (test run green-promised 4 sub-steps before the tests are updated), 14 (checkpoints run/commit tests written 3 sub-steps later), 8 (predicted `verify` failure that won't happen), 16 (DoD requires smoke.sh/requests.http never built), 27/28 (DoD requires ADRs no sub-step authors), 6 (false "can't run yet" claim).
**Fix:** reorder or re-scope; a few need a confirming run. Mostly pure edit.

### T8 — The measured interactivity decay curve
**Steps:** cross-consistency table: checkpoints 6–12 (01–10) → **0 (20–30)**; predicts → 1 from Step 15; you-are-here markers absent 18–30; `<details>` 17–32 → 3–7; Mermaid 3–7 → exactly 1 (Step 25: zero diagrams, zero predicts).
**Fix:** restore the markers as part of T1/T2 editing; the cross-consistency §5 list is the normalization checklist.

### T9 — Era-3 spine degradation (Steps 18–30)
**Steps:** 18–30: flashcards deferred to `docs/flashcards.md` instead of inline (contract: 3–5 in-lesson); DoD demoted from checklist to run-on prose; Big-Idea analogies missing (19–30); build-opening what-we-will-build diagrams and closing sequence diagrams missing; sub-step "N of M" totals lost; also sub-step count mislabels ("0 of 6" = 7 sub-steps) in 10, 11, 13–16.
**Fix:** pure edit; content for flashcards already exists in `docs/flashcards.md`.

### T10 — Dead references and unresolvable jargon
**Steps:** ~29 lessons point at `solutions/step-NN/` (only `step-01` exists); §12.3/§12.4/§8.1/§12.8 spec numbers used without a one-line gloss (7, 9, 16, 19, others); meta-docs reference nonexistent capsules, `VERIFICATION-LEDGER.md`, `concepts/`, an Anki CSV, and a MASTER_PROMPT file that resolves nowhere at any tag.
**Fix:** de-reference or create; add micro-recap at first § use. Pure edit (creation of solutions is separate work).

### T11 — Windows portability failures in a Windows-hosted course
**Steps:** 4, 12, 15, 22, 28 (bash-only `VAR=x ./mvnw` prefixes, `grep`/`head`, macOS `open`), plus single-quoted-JSON curls.
**SWE:** the stated primary platform can't run the printed commands.
**Fix:** add PowerShell variants or a "use Git Bash" banner per build. Pure edit.

### T12 — Concrete technical errors taught as fact
**Steps:** 5 (`@ConditionalOnProperty` "needs Boot" claim), 8 (impossible H2 swap-in), 9 (optimistic-lock narrative describes SQL the test never runs), 14 (`Deprecation` header attributed to RFC 8594; obsolete draft syntax), 22 (Redis "read-after-write isn't instant" taught 4×), 24 (retry config contradicts the described lock conflict), 17 ("SS7" for Spring Security 7 in an auth lesson), 13 (error detail leaks account balance against its own Security Lens).
**Fix:** each is a targeted rewrite; about half need a confirming run first.

### T13 — Working-memory bombs: 8–12 new tokens in one sub-step
**Steps:** 2 (streams), 4 (opcodes), 7 (first web layer, 4 files), 8 (7 test annotations), 9 (5 JPA tokens), 16 (10 security DSL tokens; `cors` never explained), 17 (8 JOSE types), 24 (5 classes in one paragraph), 28 (8 starter artifacts), 29 (whole TS/React stack-shift jargon undefined).
**Fix:** split sub-steps, add mini-legends/tables. Pure edit.

### T14 — Template drift across three authoring eras
**Steps:** boundaries at 09→10 and 17→18: header formats, mini-TOC list vs table, four D-header variants, DoD placement, recap label drift, dead `toc` anchors, Step 18's duplicate `build` anchor, Step 27's line-wrapped DoD.
**Fix:** normalize on one form per element (cross-consistency §5 priority list). Pure edit.

### T15 — Meta-docs staleness and single-source-of-truth violations
**Scope:** repo-level. PROGRESS.md is 65% forensic ledger, stale at the checked-out HEAD, undated, and 20× its own token budget; CLAUDE.md/docs/ai/GUIDE are untracked (invisible at every tag); README/COURSE contradict CAPABILITIES.md on `kind`; versions duplicated in ≥4 files against a declared "ONLY place" rule; flashcards over-claim CSV/spaced-repetition mechanics; the 31-of-67 authoring frontier is undisclosed to learners.
**Fix:** commit the AI-ops layer, extract the ledger, add dated headers + a tags-win self-check, de-duplicate versions, disclose the frontier. Pure edit + process.

---

## 4. Per-lens analysis

**SWE.** Where code is shown, it is remarkably good — tag-verified against the repo, Boot-4/Java-25-correct, with honest version-migration storytelling (Steps 1–9 average 8.0). The lens degrades not because the code goes wrong but because it disappears: from Step 10 the lessons increasingly describe systems instead of building them, while checkpoints, DoDs, and Verification Logs keep asserting outcomes the reader can't reproduce. The second-order SWE problem is evidence integrity: expected outputs that a real terminal will never print (`-q` silencing, summarized curl transcripts, reordered Maven tails), sequencing bugs that guarantee failure at the printed moment, and a dozen specific technical misconceptions (T12) in otherwise-accurate prose. The repo itself is the safety net: every audited artifact that *was* checked matched its tag, so re-enrichment is low-risk transcription.

**Pedagogy.** The Understand movements are the course's crown: accurate, misconception-driven, interview-aligned, with strong prior/forward step threading. But the instructional mechanics thin out in the exact same curve as the code: predicts, knowledge-checks, break-its, analogies, and worked examples all decay to ~1 per lesson by Phase D, and scaffold fading is absent course-wide — no lesson ever asks the learner to write code before the end-of-step exercises. Several objectives have no aligned retrieval item (9, 10, 15, 17), and late lessons introduce whole vocabularies (React/TS in 29) with zero first-appearance definitions. The pedagogy fix rides on the thin-build fix: once code exists, the toolkit (predict → run → check → fade) has something to attach to.

**ADHD.** The weakest lens in every phase, and the most uniform failure: not one of 30 lessons has a session plan, sub-lesson time-box, or checkpoint re-entry line, despite whole-step estimates of 8–22 hours. Early lessons at least deliver dense reward loops (runs, checkpoints, you-are-here markers every sub-step); from Step 18 those vanish too — eleven straight lessons have zero checkpoints, and multiple builds run 10+ hours before anything visibly executes. Optional content is never time-priced, walls of text peak at ~900–1,000 words in Understand movements (11, 14, 16, 17), and progress denominators are wrong ("0 of 6" for seven sub-steps). The fixes are cheap, template-shaped, and identical across lessons — which is exactly why they should be fixed as one course-wide pass.

---

## 5. Cross-cutting reports (integrated)

**Consistency (`cross-consistency.md`).** The 30 lessons were authored in three template eras with breaks at 09→10 and 17→18; nearly every structural deviation aligns to those boundaries (header forms, mini-TOC list→table, DoD demotion, sub-step heading form losing "of M", recap label drift). Its marker-count table is the quantitative backbone of this report's decay claims: lines 1,200–1,985 → 304–493; checkpoints zero from Step 20; Step 25 uniquely has zero predicts *and* zero diagrams. Point deviations: Play With It missing in 10–11; experiments missing 09–11; Step 18's duplicate `build` anchor; Step 27's line-wrapped DoD phrase; a dead `toc` anchor in 10–30. Its §5 normalization list is adopted here as the T14 checklist.

**Meta-docs (`cross-metadocs.md`).** Entry path and link hygiene are genuinely good (anchors verified, Makefile excellent), but the resume machinery is broken in practice: PROGRESS.md said "next: Step 31" while `step-31-end` already existed and the repo sat on a stale detached HEAD — the exact failure its own guide predicts. The AI-ops layer (CLAUDE.md, docs/ai/, GUIDE) is untracked and therefore invisible at every tag; it references artifacts that don't exist (capsules, VERIFICATION-LEDGER.md, this audit's own backlog — now created). Learner-facing docs promise `solutions/step-01..67`, `concepts/`, 67 openable steps, and an Anki CSV that don't exist, and contradict CAPABILITIES.md on `kind`. Priority actions: fix resume state, commit the AI-ops layer, extract the ledger from PROGRESS.md, de-reference or create the missing artifacts, disclose the authoring frontier.

---

## 6. Method note

- **Rubric:** three lenses (SWE correctness/completeness, pedagogy, ADHD-friendliness), each scored 1–10 per lesson, plus a structure check against the course's lesson contract (six movements, sub-step micro-anatomy, spine elements); `thinBuild` flags a build movement that cannot be completed from the lesson alone.
- **Audited:** 2026-07-02, against the working tree and step tags (`step-NN-start`/`step-NN-end`). Step 31 excluded (work-in-progress, untracked at audit time).
- **Process:** one auditor pass per lesson (30 files, `findings/step-NN.md`), two cross-cutting passes (structural consistency across lessons; meta-docs), and this synthesis. Findings marked `needsRun:true` require executing code to fix honestly — the repo never invents command output; those are collected as work orders in `IMPROVEMENT-BACKLOG.md`.
- **Caveat:** severity counts are per-auditor judgment; scores are comparable within this audit, not calibrated to an external scale.
