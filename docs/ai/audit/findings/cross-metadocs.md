# Cross-meta-docs audit — README / COURSE / PROGRESS / CAPABILITIES / VERSIONS / flashcards / Makefile

> Audited: 2026-07-02 · Auditor: AI meta-docs pass (two audiences: human learner, resuming AI session).
> Working-tree state at audit time: **detached HEAD at `76df0aa` (= `step-30-end`)**, while tags `step-31-start`/`step-31-end` and later commits (`34efb35`, `849c81d` on `main`) exist. `CLAUDE.md`, `docs/GUIDE-FOR-HUMANS.md`, and `docs/ai/` were **untracked** (in no commit or tag).

---

## 1. Learner usability

### 1.1 Entry path — GOOD
- README → `steps/step-01/lesson.md` is stated at least 4 times (TOC target, "Get started", "Where to go next", closing line). COURSE.md header repeats it. Unambiguous.
- `steps/step-01/` has the full promised kit (`lesson.md`, `requests.http`, `smoke.sh`).
- Makefile is high quality: 40+ documented targets (`help`, `doctor`, `verify`, `run-*`, `play-01..play-30`, `frontend-*`, `mutation`, `format`, `clean`), each with a `##` help line, and README promises raw-command equivalents (kept, spot-checked).

### 1.2 Anchors and intra-repo links — GOOD (verified)
- COURSE.md and README TOC anchors were spot-checked against GitHub slugger rules, including the hard cases: variation-selector emoji (`#️-level-badges`, `#%EF%B8%8F-the-journey-roadmap`), em-dash double-hyphens (`#phase-a--foundations-tools-language--platform-`), apostrophes (`#-what-this-course-makes-you-and-what-it-deliberately-doesnt-drill`). All resolve. Rare positive — someone did this carefully.
- Cross-file links resolve: `VERSIONS.md` → `adr/0002-…`, PROGRESS → `security/risk-register.md`, COURSE → `docs/flashcards.md`, README → `steps/step-01/lesson.md`.

### 1.3 Referenced files that DO NOT exist — HIGH impact
| Reference | Where | Reality |
|---|---|---|
| `concepts/intellij-idea.md` | README §"Your editor" ("consolidated in `concepts/intellij-idea.md` as the course grows") + `concepts/` in README repo-layout | **`concepts/` directory does not exist at all** |
| `solutions/step-NN/` for NN ≥ 2 | README layout (`solutions/step-01../step-67/`), COURSE.md tracker intro + "Where To Go Next", and **~29 lessons** (e.g. `steps/step-02/lesson.md:1869`, `step-03:1147`, `step-04:1184`, `step-05:1503`, `step-06:1246`…) | Only `solutions/step-01/` exists (containing a single README.md). Every "try first, then compare" pointer from Step 2 onward is a dead end. Some lessons also mention "the `solutions` branch" — no such branch. |
| `steps/step-32..step-67` | COURSE.md tracker lists all 67 steps as if openable; README sells "steps/step-01../step-67/" | Only steps 01–31 exist. **Nowhere in README/COURSE is it disclosed that 36 of 67 steps are not yet authored.** A learner past Step 31 falls off a cliff with no signpost. |
| `ml/`, `infra/`, `k8s/`, `helm/`, `observability/`, `mesh/`, `gitops/`, `terraform/`, `platform/`, `.github/workflows/`, most of `services/*` | README "Repository layout" block | Not present. The layout describes the **final** state without saying so ("added Phase F/I" annotations exist in VERSIONS.md but not here). |
| `requests.http` + "Bruno/Postman" per step | README layout ("per step: lesson.md + requests.http + Bruno/Postman + smoke.sh") | Drifted: `steps/step-30/` = lesson + smoke.sh only; `steps/step-31/` (at tag) = **lesson.md only, no smoke.sh**. No Bruno/Postman collections found for recent steps. |

### 1.4 README/COURSE contradict CAPABILITIES.md on `kind` — concrete staleness bug
- README:190 — "On this reference machine there is **no `kind`/`minikube`** … Kubernetes is verify-adjacent."
- COURSE.md:248 — "Kubernetes is verify-adjacent in this sandbox (**no local cluster** — see CAPABILITIES.md)."
- CAPABILITIES.md (updated 2026-06-09, re-verified 2026-06-10) — "🟢 Kubernetes — **now runnable locally** … `kind` v0.32.0 **is installed** … the live path is now available on this machine."
- The SSOT was updated; the two documents that point at it were not. Exactly the drift pattern the pinning rules are supposed to prevent.

### 1.5 Minor learner items
- README layout comment for the Makefile lists 7 targets ("doctor, verify, build, test, run-hello, play-01, clean"); there are 40+. Harmless if read as examples, stale if read literally.
- COURSE.md progress-tracker checkboxes are learner-owned ("Tick the box as you finish") — but `docs/ai/LESSON-CHECKLIST.md` orders the authoring AI to tick them as bookkeeping. Two owners for one checkbox; today all 67 are unticked despite 31 steps authored, so whichever convention is intended, it isn't being followed.

---

## 2. AI-resume usability

### 2.1 PROGRESS.md is 2 documents fused — quantified
- Whole file: **33,555 bytes, 69 lines ≈ 8.5–10k tokens** (emoji-dense, so tokens skew high).
- **Verification ledger (lines 26–59): 21,723 bytes ≈ 65% of the file** — 23 rows of per-tag forensic detail an AI resuming Step 32 should never need.
- "Where we are": single-line bullets of 1,766 chars (line 9) and 1,651 chars (line 17, "Next action"); "Done so far" line 24 is a **3,982-char single line** that narratively duplicates the ledger.
- The genuinely resume-critical content (phase, current step, last verified tag, next action) is ≈ 3–4k chars ≈ **~1k tokens — about 11% of the file**.
- The same facts about each step appear **three times inside PROGRESS.md alone** (a "Step (prior)" bullet, the "Done so far" paragraph, a ledger row) — plus a fourth copy in the lesson's Verification Log.
- `docs/ai/CONTEXT-PLAYBOOK.md` budgets PROGRESS.md at "≤ ~40 lines, ~400 tokens". Actual: ~69 lines, ~20× the token budget. **The playbook's read-set arithmetic is fiction until the ledger moves out.**

### 2.2 "Next action" is explicit but NOT current — the flagship failure
- Working-tree PROGRESS.md says: Step 30 complete, **"Next action: Step 31"**.
- Tags `step-31-start` and `step-31-end` already exist; `main`'s tip (`849c81d`) is 2+ commits ahead; PROGRESS.md **at** `step-31-end` correctly says "Next action: Step 32".
- Because PROGRESS.md is versioned, its truth depends on which commit is checked out — and the repo was left on a **stale detached HEAD** (the exact failure mode GUIDE-FOR-HUMANS' troubleshooting table predicts). A fresh session obeying "read THIS file first" would re-do a completed, tagged step.
- PROGRESS.md carries **no timestamp and no self-check** ("if `git tag` shows a newer `step-NN-end` than this file, trust the tags"). One header line would defuse the whole failure mode.

### 2.3 The AI-ops layer references artifacts that don't exist
| Referenced | By | Reality |
|---|---|---|
| `docs/ai/VERIFICATION-LEDGER.md` | CLAUDE.md (routing table, rule 8, repo map: "ledger **moved** to docs/ai/"), GUIDE-FOR-HUMANS (×2), CONTEXT-PLAYBOOK (×2), LESSON-CHECKLIST | **Does not exist.** The ledger still lives inside PROGRESS.md. CLAUDE.md's "moved" claim is false today. |
| `steps/step-NN/capsule.md` (≤25 lines, "the memory") | CLAUDE.md rule 8 + repo map, GUIDE ("Capsules are the memory"), CONTEXT-PLAYBOOK read-set item #2 | **Zero capsules exist** (checked step-01, step-30, step-31 incl. at tag). The playbook's fallback ("if the capsule doesn't exist… backfill") will trigger on *every* step. |
| `docs/ai/audit/IMPROVEMENT-BACKLOG.md` | GUIDE-FOR-HUMANS (quoted prompt) | Does not exist; `docs/ai/audit/` holds only `findings/`. GUIDE also promises a "report" — absent. |
| "MASTER_PROMPT" | PROGRESS.md next-action ("per MASTER_PROMPT Part IV… Re-read MASTER_PROMPT §8/§8.1/§12"); CAPABILITIES ("Operating Contract §4", "§12.8", "Guardrails §14") | No file named MASTER_PROMPT*. The mapping to `build-a-bank-claude-code-prompt.md` exists only in CLAUDE.md/GUIDE — which are **untracked and in no tag**. At any checked-out tag, `§12.3`/`§8` are unresolvable symbols. |
| CLAUDE.md, docs/ai/, GUIDE-FOR-HUMANS | each other | All untracked → invisible at every tag, unprotected by git, and absent for any clone. Commit them. |

### 2.4 Duplication across README / COURSE / master prompt
- Fast-track routes: full table in README + condensed restatement in COURSE (§Skill Tree note). Milestones: README table + COURSE table + COURSE mermaid + per-phase callouts (4 renderings of the same 8 facts).
- Total meta-doc read for a naive resuming AI: README 22k + COURSE 32k + PROGRESS 34k + master prompt 98k chars ≈ **46k+ tokens** before touching code. The CLAUDE.md routing table is the right fix — but it's untracked and points at missing files (§2.3).
- Master prompt is 97,978 bytes (~24–26k tokens); CLAUDE.md calls it "35k tokens" — minor, but a stated number that will drift.
- Declared sync pairs ("update BOTH the master prompt and LESSON-SPEC"; PROGRESS mirrors ledger rows) are standing dual-maintenance costs with no verification mechanism.

### 2.5 Single-source-of-truth violations (versions, counts)
- CLAUDE.md: "`VERSIONS.md` — the **ONLY** place version numbers live." Violated by design everywhere:
  - README: badges (Java 25.0.3, Boot 4.0.6, Maven 3.9.12) + pinned-stack table (those + Spring Cloud 2025.1.1, Tomcat 11.0.21, Node 22.20.0/npm 11.16.0, Python 3.13.7) + prose + sample output ("spring-boot:4.0.6:repackage").
  - COURSE.md "Where To Go Next" restates 4 versions. CAPABILITIES restates the toolchain (defensible — it's a probe record). PROGRESS restates frontend dep versions (react-query 5.101 etc.).
  - A Boot patch bump or the promised Phase-I Spring-AI re-pin requires edits in ≥4 files. The `kind` contradiction (§1.4) proves this drift already happens.
- "67 steps" and "≈1,340 hours" are hardcoded in README (×5+), COURSE (×4+), CLAUDE.md, PROGRESS ("of 67"), every lesson banner ("Step N of 67"). Any scope change is a ~40-file edit. Acceptable if consciously frozen; worth one line in the master prompt saying so.

---

## 3. Staleness risks
- **Dated & re-verified: CAPABILITIES.md** ("Probed: 2026-06-09", kind section "updated 2026-06-09; re-verified 2026-06-10") — the model citizen.
- **Dated once: VERSIONS.md** ("Resolved & pinned: 2026-06-09"); per-row provenance via "First used / Verified" columns — good.
- **No date at all: PROGRESS.md** (the one file whose staleness is operationally dangerous — see §2.2), **README.md, COURSE.md** (both contain volatile claims: versions, sandbox capabilities, "today" language).
- Undated "today": VERSIONS watch-item 1 and COURSE Phase-I warning both say Spring AI "only 2.0.0-RC1 … today" — unfalsifiable next year. GUIDE hardcodes "The 2026-07-02 baseline audit" for an artifact set that doesn't fully exist yet.
- README `curl` sample output embeds 2026-06-09 timestamps — fine (labeled as a real captured run).

---

## 4. docs/flashcards.md
- 48,725 bytes (~12k tokens), exactly 30 `## Step N` sections — **current through Step 30** (Step-31 cards not yet appended despite `step-31-end` existing — same lag as §2.2).
- Content quality is high (real gotchas: PITest 1.19.1 vs JDK-25 bytecode, jsdom localStorage, Spotless CRLF).
- **Not importable as claimed.** Header: "Import into Anki via the **optional CSV**" — no CSV exists anywhere in the repo. Format is markdown bullets `**Q:** … — **A:** …`; the em-dash delimiter also appears *inside* answers, so even a mechanical conversion would mis-split. No Anki `#tags`; per-step grouping is headings only.
- **COURSE.md over-claims twice** (§Cumulative Reviews): "(Anki-importable CSV)" — none; "each tagged with a **🔁 revisit in ~N steps** spaced-repetition pointer" — `grep -c '🔁' docs/flashcards.md` = **0**. The promised spaced-repetition mechanic does not exist in the deck.

---

## 5. Prioritized recommendations
1. **Fix the resume state now:** return the repo to `main`, update PROGRESS.md (Step 31 done at `step-31-end` per its own tagged copy — or reflect actual `main` tip), and add a dated header + one self-check line: "If `git tag` shows a newer `step-NN-end` than this file, the tags win."
2. **Commit the AI-ops layer** (CLAUDE.md, docs/ai/, GUIDE-FOR-HUMANS.md) — it is currently invisible at every tag and to every clone.
3. **Extract the ledger:** move PROGRESS.md lines 26–59 (and the line-24 mega-paragraph) into the already-referenced `docs/ai/VERIFICATION-LEDGER.md`; PROGRESS.md becomes the ≤40-line baton it claims to be (~1k tokens, 10× cheaper per session).
4. **Create or de-reference:** capsules (backfill or drop the claim), `IMPROVEMENT-BACKLOG.md`, `concepts/intellij-idea.md`, `solutions/step-02..31` (or change ~29 lesson pointers + README/COURSE to "solutions ship per-step going forward / see `step-NN-end` tags").
5. **Sync the `kind` story** in README:190 and COURSE:248 to CAPABILITIES.md, and add "Last verified" lines to README/COURSE.
6. **De-duplicate versions:** keep README badges if desired, but replace the pinned-stack table's numbers and COURSE's restatement with links to VERSIONS.md — or annotate CLAUDE.md's "ONLY place" rule to name the tolerated mirrors.
7. **Make flashcards honest:** either generate the CSV + 🔁 pointers, or soften the COURSE.md/flashcards.md claims to "markdown Q/A deck, self-quiz".
8. **Disclose authoring frontier to learners:** one README/COURSE line — "Steps 1–N are authored and verified; later steps are the roadmap."
