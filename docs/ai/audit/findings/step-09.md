# Step 9 audit - swe:8 pedagogy:8 adhd:6 structure:8 - thinBuild:false

## Strengths

- The lesson code is the real, tag-verified repo code: `Address.java`, `Customer.java`, both test classes and `smoke.sh` at tag `step-09-end` match the lesson listings byte-for-byte, imports use the correct Spring Boot 4 modularized packages, and all commands fit the multi-module layout (`./mvnw -pl services/cif -am verify`).
- Proof-driven teaching at its best: N+1 is asserted with Hibernate statistics (3 vs 1), the lost update with a real conflict on Testcontainers Postgres, and both proofs have predict-then-run, a 60-second break-it-on-purpose, and a mutation sanity-check (remove `@Version`, watch the test fail, revert).
- Strong continuity scaffolding: the Step-8 "OSIV off, explained in Step 9" promise is explicitly cashed in, forward references to Steps 10-12 are precise, and every one of the six sub-steps carries a you-are-here breadcrumb.

## Missing spine

- `requests.http` is absent from `steps/step-09/` — intentionally: the step adds no HTTP endpoints, and the lesson justifies this in the Orient table, the Starting Point note, and Play With It. All six movements and every required lesson subsection are present; TOC anchors (#orient, #understand, #build, #prove, #apply, #review) all resolve.

## Findings

### F1: No session plan, per-movement time-boxes, or re-entry lines for a ~20-hour step

**Severity:** high
**Lens:** adhd
**Location:** A · Orient, "Before You Start" (lines 104-119) and every ✋ Checkpoint in C · Build
**needsRun:** false
**Issue:** The step advertises ≈20h of focused effort but offers no sitting plan (a 20h step needs 6-10 planned sittings of ~2-3h with named save points), no time-boxes per movement or per sub-step (only the whole-step 20h and two "60s" break-its), and no re-entry support at checkpoints — a learner returning after three days has no "you have X working; next session starts at sub-step N, first action: ..." line anywhere.
**Fix:** (1) Add a "Session plan" table to Before You Start splitting the step into ~7 sittings with named save points, e.g. S1 Orient+Big Idea (~2h, save: skip-test done), S2 rest of Understand (~2.5h, save: spotlight read), S3 sub-steps 1-2 (~2.5h, save: commit "add @Version and lazy @OneToMany"), S4 sub-step 3 + verify (~2h), S5 sub-step 4 (~2h), S6 sub-step 5 + break-it (~3h), S7 sub-step 6 + Prove + Review (~3h). (2) Add "≈ Nh" tags to each movement heading and "⏱ ~Nm" to each sub-step heading. (3) Append one re-entry line to each ✋ Checkpoint: "Stopping here? Everything committed is green; next session: sub-step N — first action: open <file>."

### F2: Optimistic-lock narrative describes an UPDATE-row-count path the test likely never executes

**Severity:** high
**Lens:** swe
**Location:** Sub-step 6 line-by-line (lines 1014-1018), under-the-hood (line 1018), the second sequence diagram (lines 1089-1104), and Prove §3 (line 1186)
**needsRun:** true
**Issue:** The test calls `repository.save(userB)` on a *detached* entity, which routes through `EntityManager.merge()`. Hibernate's merge listener re-loads the current row (a SELECT) and compares versions; on mismatch it throws `StaleObjectStateException` at merge time — *before* any `UPDATE ... WHERE version=0` is issued. The lesson's line-by-line ("Hibernate runs `UPDATE … WHERE id=? AND version=0` — 0 rows match → throws"), the sequence diagram ("DB→B: 0 rows matched"), and Prove §3 ("B's stale `UPDATE … WHERE version = 0` matches 0 rows") narrate SQL that most likely never runs for user B. The exception and the lesson's conceptual claim are correct, but a step whose brand is "no magic, watch the real SQL" asserts an execution path as observed evidence without observing it.
**Fix:** Run `OptimisticLockingTest` with SQL statement logging (`spring.jpa.show-sql=true` or datasource-proxy) and confirm which path fires for user B. Then either (a) correct the prose and diagram: "`save()` on a detached entity merges: Hibernate re-reads the row, sees version 1 ≠ B's 0, and throws before issuing the UPDATE; the `WHERE id=? AND version=?` row-count check is the flush-time guard for *managed* entities racing to commit", keeping the row-count mechanism as the general explanation in B · Understand; or (b) restructure the test so the row-count path genuinely fires (two concurrent transactions each holding a managed copy), and paste the observed SQL.

### F3: Verification Log sections 3 and 5 have no pasted output — prose claims only

**Severity:** medium
**Lens:** structure
**Location:** D · Prove §3 (lines 1184-1186) and §5 (lines 1200-1206); §1 (lines 1158-1165) is heavily abridged
**needsRun:** true
**Issue:** The log promises "the real, pasted evidence from this machine" at 🔴 Full tier, but §3 (optimistic-lock conflict) is a prose paragraph with a ✅ and no terminal output, and §5 (`smoke.sh`) shows only the command and a description of what it prints, not the actual run. §2's SQL is honestly labelled "illustrative", but the statistics numbers are also only described, never shown as raw surefire/console lines.
**Fix:** Re-run `./mvnw -pl services/cif test -Dtest=OptimisticLockingTest` and `bash steps/step-09/smoke.sh`, and paste the real surefire summary block for §3 and the real terminal output (build line + "✅ Step 9 smoke test PASSED") for §5. Optionally paste the actual `Hibernate:` SQL lines captured with `show-sql` into §2 in place of the illustrative block.

### F4: Sub-steps 1, 2, and 4 have no run-and-see — up to ~250 typed lines between feedback

**Severity:** medium
**Lens:** structure
**Location:** Sub-step 1 checkpoint (line 383), sub-step 2 predict/checkpoint (lines 562-564), sub-step 4 checkpoint (line 700)
**needsRun:** true
**Issue:** The micro-anatomy contract requires run-and-see for every sub-step, and the reward-loop suffers: after the baseline verify, the learner types Address.java (~75 lines) and the full Customer.java (~145 lines) with no command to run — only "compiles (no red squiggles)", which assumes an IDE the lesson never requires. The lesson correctly explains why full `verify` can't run before V2 (validate would fail), but a compile-only loop is available and unused.
**Fix:** Add a ▶️ Run & See to sub-steps 1, 2, and 4: `./mvnw -pl services/cif -am test-compile` with the real expected tail (`BUILD SUCCESS`) and one common-wrong-output each (e.g. sub-step 2: `cannot find symbol: method setCustomer` if Address.java wasn't saved with the package-private setter). Run the command once per sub-step state to paste truthful output.

### F5: No scaffold fading — all six sub-steps are fully worked paste-ins with zero type-it-yourself

**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, sub-steps 1-6 (lines 282-1054)
**needsRun:** false
**Issue:** Every sub-step hands over complete final code; the interactivity toolkit's type-it-yourself never appears, and scaffolding never fades. By sub-step 6 the learner has seen the @Version mechanism explained four times (Spotlight, Under the Hood, sub-step 2, Prove) yet is still given the whole test verbatim — a missed opportunity for the strongest retrieval event of the step.
**Fix:** Convert sub-step 6 into guided type-it-yourself: give the class skeleton (annotations, fields, `init()`, the seed transaction) and bullet prompts ("write transactions 2-3: two independent reads; write A's committing update; write the assertion that B's save throws — which exception class?"), with the full solution in a collapsed `<details>` labelled "Stuck? full file". Do the same for `CustomerSummary` in sub-step 4 (3 getters — trivially self-writable from the projection rules just taught).

### F6: Edits shown as full-file re-listings instead of diff view

**Severity:** medium
**Lens:** structure
**Location:** Sub-step 2, Customer.java (lines 402-548); sub-step 4, CustomerRepository.java (lines 650-686)
**needsRun:** false
**Issue:** The contract requires diff view for edits. Customer.java is a ~145-line full re-paste with the new pieces identified only in prose; the learner must visually diff to find the ~35 changed lines, and re-pasting the whole file invites drift if their Step-8 file differed slightly (e.g., their own comments).
**Fix:** Present each edit as a unified diff (```diff with `+` lines for the new imports, `@Version` field, `@OneToMany` block, getters, and `addAddress`) or as 3 short "add this block after X" snippets, and move the full post-edit file into a collapsed `<details>` titled "Full file after the edit (for checking)".

### F7: "§12.3 mutation sanity-check" referenced five times but never defined

**Severity:** low
**Lens:** adhd
**Location:** Lines 22, 41, 1034, 1119, and the Prove §4 heading (line 1188)
**needsRun:** false
**Issue:** The label "§12.3" (a course-wide verification-spec section) is jargon used before — in fact, never — defined in this lesson. A learner hits it in the Six Movements map and the Orient table with no way to resolve it, a working-memory tax on every occurrence.
**Fix:** At first use (line 22 or the Orient table row), add a parenthetical with a link: "the **§12.3 mutation sanity-check** (course verification spec §12.3: temporarily remove the guard, watch the test fail, revert — proving the test isn't vacuous; see docs/<spec-file>)". Later occurrences can then stay bare.

### F8: Definition of Done requires tagging step-09-end but the tag command is never given

**Severity:** low
**Lens:** structure
**Location:** DoD checklist (line 1148) vs 🏁 The Finished Result (lines 1131-1138)
**needsRun:** false
**Issue:** The final DoD item is "You've committed and tagged `step-09-end`", but no sub-step or closing section contains a `git tag` command — the last instructed action is sub-step 6's test commit. A beginner cannot satisfy the checklist from the lesson alone.
**Fix:** Add a final command block to 🏁 The Finished Result: `git tag step-09-end` (preceded by `git status` to confirm a clean tree), mirroring whatever tagging convention earlier steps use.

### F9: Sub-step 2 introduces ~5 new JPA tokens in one sub-step

**Severity:** low
**Lens:** pedagogy
**Location:** Sub-step 2 (lines 396-573)
**needsRun:** false
**Issue:** One sub-step carries `mappedBy`, `cascade = CascadeType.ALL`, `orphanRemoval`, owning-vs-inverse side, and `@Version` — beyond the ~3-new-terms guideline. `cascade` and `orphanRemoval` each get a single clause yet sit in the headline code, so a beginner must accept two behaviors on faith while also absorbing the versioning concept.
**Fix:** Either split into 2a (relationship: `@OneToMany`, `mappedBy`, `addAddress`) and 2b (`@Version` only — it is the step's second headline concept and deserves its own goal/predict/checkpoint), or keep one sub-step but add a 2-row mini-table defining cascade and orphanRemoval with a one-line concrete example each, and defer the rest to a Go-Deeper entry.

### F10: Wall-of-text in the Big Idea analogy and Pattern Spotlight

**Severity:** low
**Lens:** adhd
**Location:** Librarian analogy blockquote (line 131, ~210 words unbroken); Pattern Spotlight (lines 161-171, five consecutive dense blockquotes, ~400 words, no visual)
**needsRun:** false
**Issue:** The librarian analogy is a single unbroken paragraph carrying five mapped concepts; the Optimistic-Locking spotlight is five back-to-back quote blocks with no diagram, table, or code between them — the two heaviest reading stretches in the lesson.
**Fix:** Break the analogy into four bulleted beats (desk copy = 1st-level cache; pencil-mark watching = dirty checking; closing-time copy-back = flush; slip-cover stand-ins = lazy proxies) with the `@Version` edition-stamp as a fifth. In the spotlight, replace the Alternative paragraph with a two-column optimistic-vs-pessimistic table (locks held / conflict detection / best when / cost).

### F11: Projection objective has no retrieval item in Test Yourself or flashcards

**Severity:** low
**Lens:** pedagogy
**Location:** Outcome "Trim over-fetching..." (line 100) vs 🧠 Test Yourself (lines 1396-1401) and 🃏 Flashcards (lines 1424-1430)
**needsRun:** false
**Issue:** Every other outcome maps to at least one test-yourself question and flashcard, but projections — a stated objective, a built artifact (`CustomerSummary`), and interview Q6 — appear in neither, so the objective has no retrieval-practice alignment.
**Fix:** Add flashcard "Q: What does a closed interface projection do to the generated SQL, and why is that a security win? | A: Spring Data selects ONLY the projected columns (no full-entity hydration, no lazy associations) — data minimization: you can't leak a column you never loaded." Optionally add a fifth Test Yourself question asking when a projection beats an `@EntityGraph` fetch.
