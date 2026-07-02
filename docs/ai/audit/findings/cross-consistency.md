# Cross-Lesson Consistency Audit — steps/step-01..step-30/lesson.md

Date: 2026-07-02 · Method: structural grep/awk comparison across all 30 lessons (no deep read).
Scope note: `steps/step-31/` exists (untracked, 125 lines) but is outside this audit's 30-lesson scope.

---

## 1. Headline finding: three distinct template "eras"

The lessons were clearly authored in three batches with template drift at two boundaries — **step 09→10** and **step 17→18**. Almost every deviation below aligns to one of these boundaries.

| Feature | Era 1 (01–09) | Era 2 (10–17) | Era 3 (18–30) |
|---|---|---|---|
| Top header | `# Step N · Title` + blockquote `> **Step N of 67 · Phase …** · Level badge … · Effort ≈ Nh` | `# Step N · Title` + `### Phase … · Step N of 67` (h3 line; no "Level badge"/inline effort) | same as Era 2 |
| Level-badge legend line (`🟢 Foundations · 🔵 Core · 🟣 Advanced · 🔴 Frontier`) | present | **absent** | **absent** |
| `> [!CAUTION]` educational disclaimer | present (step 08 has 4 CAUTION blocks) | **absent** | **absent** |
| Six Movements mini-TOC | numbered **list** (`1. **[A · 🧭 Orient](#orient)** — …`) | 3-column **table** (`| **A** | [🧭 Orient](#orient) | …`) | table |
| `<a id="toc">` anchor | absent (6 anchors: orient…review) | present (7 anchors) | present |
| `# C ·` header | `🛠️ Build` (except 02: `🛠️ Build — Let's Build It, Step by Step`) | `🛠️ Build` | `🛠️ Let's Build It — Step by Step` |
| `# D ·` header | mixed: `🔬 Prove` (01, 05), `🔬 Prove It Works` (06), `🔬 Prove It Works — the Verification Log` (02–04, 07–09) | `🔬 Prove It Works — the Verification Log` | `🔬 Prove It Works — Verification Log` (drops "the") |
| Sub-step heading | `### Sub-step N of M — Title` (h3, with total) | same | `## Sub-step N — Title` (**h2, no "of M" total**) |
| Verification-tier | table row in "30 Seconds" **plus** standalone `> **Verification tier: …**` statement in D | **table row only** — standalone statement dropped | table row only |
| Definition of Done | `### ✅ Definition of Done (your self-check)` heading (01–04); **inline bold** variants in 05–08 (`✅ **Definition of Done** — …`, `**✅ Definition of Done** (the learner's se…`, `✅ **Definition of Done (learner self-check…`) | `### ✅ Definition of Done (your self-check)` heading | **demoted to an inline sentence** inside "🏁 The Finished Result" (`**✅ Definition of Done:** …`; 18–19 say `**✅ Learner Definition of Done:**`) |
| F-section headings | `## 📚 Learn More: Resources & Glossary`, `## 💼 Interview Prep: Questions…` | same | `## 📚 Learn More & Glossary`, `## 💼 Interview Prep` |
| Recap item (d) | `🔗 How This Connects` (Title Case) | `🔗 How this connects` (lower case, from step 10) | lower case |
| Recap item (e) | `🏆 Résumé line / interview talking point` | same | `🏆 Résumé line` (shortened) |
| Recap item (i) | `(i) 🎉 Sign-off` (01–08) | `(i) Sign-off` (09–17, 🎉 dropped from label) | `**(i)**` with no label at all (🎉 moved into body text) |
| Recap item (f) | `✅ You can now…` (ellipsis) | same | `✅ You can now` / `You can now:` (ellipsis dropped) |

Consistent across all 30 (no drift): `### ⏭️ Can You Skip This Step? (5-minute self-check)` (exact text, all 30) · `## 📇 Cheat Card` · `**Depends on**` table row · `## 🧭 The Six Movements of This Step` heading text · `## 🩺 Stuck? Troubleshooting` · `## 🏋️ Your Turn: Practice & Challenges` · `## 🏆 Recap & Study Notes` (step 12 adds a suffix "— a…") · recap letters (a)–(i) skeleton · flashcards + `docs/flashcards.md` reference · one-line reflection · 🔁 Revisit spaced-repetition note (though count drops 2→1 at step 18).

---

## 2. Per-step marker-count table (the interactivity decay curve)

Marker vocabulary note: the course has no literal "knowledge-check" or "run-and-see" tokens. The concrete equivalents counted here: **predict** = `🔮 **Predict`; **checkpt** = `✋ **Checkpoint`; **exper** = `🧪 Little experiments`; **youhere** = "you are here" ASCII-diagram callouts; **play** = `## 🎮 Play With It` section; **details** = `<details>` collapsibles (predictions/quizzes); **mermaid** = diagrams; **substeps** = build sub-step count. `🧠 Test Yourself` quiz exists in all 30 (constant, not shown).

| step | lines | predict | checkpt | exper | youhere | play | details | mermaid | substeps |
|---|---|---|---|---|---|---|---|---|---|
| 01 | 1254 | 6 | 8 | 1 | 1 | 1 | 17 | 4 | 8 |
| 02 | 1971 | 12 | 12 | 1 | 1 | 1 | 32 | 3 | 12 |
| 03 | 1297 | 5 | 6 | 1 | 1 | 1 | 18 | 6 | 6 |
| 04 | 1319 | 7 | 7 | 1 | 1 | 1 | 18 | 7 | 7 |
| 05 | 1598 | 10 | 11 | 1 | 2 | 1 | 28 | 4 | 11 |
| 06 | 1335 | 5 | 7 | 1 | 1 | 1 | 20 | 5 | 6 |
| 07 | 1458 | 7 | 7 | 1 | 2 | 1 | 19 | 5 | 7 |
| 08 | 1985 | 7 | 12 | 1 | 1 | 1 | 23 | 4 | 12 |
| 09 | 1440 | 6 | 7 | **0** | 1 | 1 | 27 | 5 | 6 |
| 10 | 1200 | 7 | 7 | **0** | 1 | **0** | 12 | 4 | 7 |
| 11 | 690 | 1 | 3 | **0** | 1 | **0** | 9 | 4 | 4 |
| 12 | 835 | 4 | 7 | 1 | 1 | 1 | 20 | 4 | 7 |
| 13 | 812 | 3 | 7 | 1 | 1 | 1 | 12 | 4 | 7 |
| 14 | 741 | 2 | 7 | 1 | 1 | 1 | 11 | 4 | 7 |
| 15 | 684 | 1 | 6 | 1 | 1 | 1 | 9 | 4 | 6 |
| 16 | 652 | 1 | 5 | 1 | 1 | 1 | 10 | 4 | 5 |
| 17 | 639 | 2 | 5 | 1 | 1 | 1 | 11 | 4 | 5 |
| 18 | 493 | 3 | 2 | 1 | **0** | 1 | 7 | 2 | 5 |
| 19 | 372 | 1 | 1 | 1 | 0 | 1 | 6 | 2 | 5 |
| 20 | 379 | 1 | **0** | 1 | 0 | 1 | 5 | 1 | 4 |
| 21 | 349 | 1 | 0 | 1 | 0 | 1 | 4 | 1 | 5 |
| 22 | 343 | 1 | 0 | 1 | 0 | 1 | 5 | 1 | 3 |
| 23 | 323 | 1 | 0 | 1 | 0 | 1 | 4 | 1 | 3 |
| 24 | 326 | 1 | 0 | 1 | 0 | 1 | 4 | 1 | 3 |
| 25 | 304 | **0** | 0 | 1 | 0 | 1 | 3 | **0** | 4 |
| 26 | 339 | 1 | 0 | 1 | 0 | 1 | 4 | 1 | 4 |
| 27 | 399 | 1 | 0 | 1 | 0 | 1 | 5 | 1 | 4 |
| 28 | 428 | 1 | 0 | 1 | 0 | 1 | 5 | 1 | 6 |
| 29 | 384 | 1 | 0 | 1 | 0 | 1 | 5 | 1 | 6 |
| 30 | 362 | 1 | 0 | 1 | 0 | 1 | 5 | 1 | 5 |

**Decay summary:**
- **Lines:** 1200–1985 (steps 01–10) → 639–835 (11–17) → 304–493 (18–30). Late lessons are ~4–6× shorter than early ones.
- **✋ Checkpoints:** 6–12 per lesson (01–10) → 1–7 (11–19) → **zero from step 20 through 30** (11 consecutive lessons with no checkpoint).
- **🔮 Predict:** 5–12 (01–10) → mostly 1 from step 15 on; **step 25 has zero**.
- **"You are here" locator callouts: absent from all of 18–30.**
- **`<details>` interactive collapsibles:** 17–32 (01–09) → 3–7 (18–30).
- **Mermaid diagrams:** 3–7 early → exactly 1 late; **step 25 has zero**.

---

## 3. Presence deviations (steps that break an otherwise-universal element)

1. **`## 🎮 Play With It` missing in steps 10 and 11** (present in the other 28; step 10 mentions the phrase but has no section).
2. **🧪 "Little experiments" missing in steps 09, 10, 11** (present in 01–08 and 12–30).
3. **Step 27 "Definition of Done" is hard-wrapped mid-phrase** (`**✅ Definition` / newline / `of Done:**`, lesson.md:257–258) — the only lesson where the phrase doesn't survive a single-line search; breaks grep/tooling and any anchor-based linking. All other 29 have it on one line.
4. **Step 18 has a duplicate `<a id="build">` anchor** (defined twice) — the second definition makes the TOC "Build" link ambiguous.
5. **Dead `toc` anchor (10–30):** `<a id="toc">` is declared but no lesson contains a `(#toc)` back-link — no "back to top" navigation exists anywhere in the course.
6. **Verification-tier statement in movement D exists only in 01–09**; from 10 on, the tier is asserted only in the Orient table.
7. Step 09 recap uses `(b) Key Terms` (Title Case) vs `(b) Key terms` in the other 29.
8. Step 02 is the only Era-1/2 lesson using the long C header (`Build — Let's Build It, Step by Step`); steps 01 and 05 use bare `# D · 🔬 Prove`, step 06 `Prove It Works` — four D-header variants total across the course.

---

## 4. Anchor / TOC conventions

- Anchor ids are consistent lowercase single words (`orient`, `understand`, `build`, `prove`, `apply`, `review`); Era 1 = 6 anchors, Eras 2–3 = 7 (adds `toc`). Exceptions: step 18's duplicated `build` (above).
- Mini-TOC format flips from prose numbered list (01–09) to a `| | Movement | What happens |` table (10–30). The table body text is itself uniform across 10–30.

---

## 5. Recommended normalization targets (highest value first)

1. Restore **✋ Checkpoints** to steps 20–30 (currently zero) and **you-are-here** locators to 18–30 — the two markers that vanish completely.
2. Give step 25 at least one 🔮 Predict and one mermaid diagram (only step with zero of both).
3. Add `## 🎮 Play With It` to steps 10–11; add 🧪 experiments to 09–11.
4. Unify sub-step headings on one form (`### Sub-step N of M — Title`) — 18–30 currently lose the "of M" progress signal ADHD learners rely on.
5. Re-promote Definition of Done to its own `### ✅` heading in 05–08 and 18–30; fix step 27's line-wrapped phrase.
6. Unify `# C ·`/`# D ·` header text (4 variants), `📚 Learn More…`/`💼 Interview Prep…` names, recap capitalization (`How This Connects`, `Key terms`), and `(e)`/`(i)` labels.
7. Decide whether the `> [!CAUTION]` disclaimer + level-badge legend are per-lesson or step-01-only; today they silently stop at step 09.
8. Fix step 18's duplicate `build` anchor; either use or remove the dead `toc` anchor.
