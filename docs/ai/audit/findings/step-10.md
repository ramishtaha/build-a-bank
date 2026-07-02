# Step 10 audit - swe:7 pedagogy:8 adhd:5 structure:8 - thinBuild:false

## Strengths
- Technically rigorous and honest Prove section: real EXPLAIN/anomaly/pool output, a genuine mutation sanity-check (weaken SERIALIZABLE, watch the write-skew test fail, revert), and an explicit "a single node cannot demonstrate streaming replication" honesty note for read replicas.
- Full micro-anatomy rhythm on every sub-step (goal, location, line-by-line, under-the-hood, predict, run-and-see, checkpoint, commit, pitfall) plus you-are-here markers, break-it-on-purpose moments, and a run with visible output at the end of every lab - excellent reward-loop density.
- Strong misconception handling wired straight to interview questions: bitmap vs plain Index Scan is pre-empted with a knowledge check, "PG has no dirty reads", "PG prevents phantoms at REPEATABLE READ", and "write skew survives REPEATABLE READ" are all taught, run, and retrieved.

## Missing spine
- Play With It section in Build: `steps/step-10/queries.sql` is referenced in the files-we-will-touch tree (line 280), the cheat card, and a parenthetical in sub-step 2, but there is no dedicated Play-With-It section walking the learner through it.

## Findings

### F1: Sub-steps 3-6 ship excerpts, not complete compilable files
**Severity:** high
**Lens:** swe
**Location:** Sub-step 3 (`WriteSkewTest`, ~line 657), Sub-step 4 (`ConnectionPoolTest`, ~line 737), Sub-step 5 (`PartitioningLabTest`, ~line 813), Sub-step 6 (`OnlineSchemaChangeTest`, ~line 876)
**needsRun:** false
**Issue:** Four of seven sub-steps violate the "complete code, all imports, compiles as shown" contract. `WriteSkewTest` omits the class declaration, imports, the `linked_account` table setup/seed, and the `combinedBalance()` helper ("full file in the repo"); `ConnectionPoolTest` shows a bare body with no class, no `@Test`, no imports (`HikariConfig`, `HikariDataSource`, `HikariPoolMXBean`, `SQLTransientConnectionException`); `PartitioningLabTest` elides the seed with a comment ("... insert ~900 rows across the three months, then ANALYZE ..."); `OnlineSchemaChangeTest` omits imports/class. A nervous beginner at 11pm cannot type these and get a green run without opening the repo - the exact thing the sacred-build contract forbids.
**Fix:** Replace each excerpt with the complete file already present at `C:/Users/ramishtaha/Desktop/Claude/build-a-bank/services/cif/src/test/java/com/buildabank/cif/dblab/` (WriteSkewTest.java, ConnectionPoolTest.java, PartitioningLabTest.java, OnlineSchemaChangeTest.java), keeping the file-path header comment. If deliberate scaffold fading is intended for the later labs, keep the excerpt as the "you write this part yourself" spec but put the full reference file in a collapsed `<details><summary>Reference solution (full file)</summary>` block inside the lesson - never "full file in the repo".

### F2: Expected output shows println lines the shown code never prints
**Severity:** high
**Lens:** swe
**Location:** Sub-step 2 Run & See (~line 627-635); same pattern in sub-steps 3, 4, 6
**needsRun:** false
**Issue:** Sub-step 2's code block is labeled "(complete file)" and contains zero `System.out.println` statements, yet its Expected Output lists five bracketed lines (`[repeatable-read] first=100 second=100` etc.). The actual repo file `MvccIsolationTest.java` contains 5 printlns - the lesson's code diverged from the code that produced the evidence. A learner who types exactly what is shown sees none of those lines and concludes they broke something. Sub-steps 3, 4 and 6 have the same mismatch (bracketed `[write-skew ...]`, `[pool ...]`, `[online-ddl ...]` lines absent from the excerpts).
**Fix:** Sync every lesson code block with the actual repo file so that each line shown in Expected Output is produced by a visible `System.out.println` in the shown code (this is solved automatically by applying F1's paste-the-full-file fix; for sub-step 2 specifically, replace the code block with the repo's `MvccIsolationTest.java` verbatim).

### F3: No session plan or sub-step time-boxes for a ~20-hour step
**Severity:** high
**Lens:** adhd
**Location:** A - Orient, "This Step in 30 Seconds" / "Before You Start" (~lines 29-121)
**needsRun:** false
**Issue:** Effort is stated only as a single "~20 hours focused" figure. There is no planned-sittings structure (contract: 6-10 sittings of ~2-3h with named save points), no per-movement time-boxes, and no per-sub-step estimates. An ADHD learner has no way to answer "can I start this tonight?" or "where is a safe place to stop?".
**Fix:** Add a "🗓️ Session plan" box at the end of Orient with ~7 named sittings, each ending at an existing commit, e.g.: S1 Orient+Understand (~2.5h, save point: skip-test done); S2 Sub-steps 0-1 harness + query plans (~3h, save: commit "seq->index-only"); S3 Sub-step 2 MVCC anomalies (~2.5h); S4 Sub-step 3 write skew (~2.5h); S5 Sub-steps 4-5 pool + partitions (~3h); S6 Sub-step 6 + queries.sql play (~2.5h); S7 Prove+Apply+Review (~3h). Also append "⏱ ~Xh" to each sub-step heading and each movement heading.

### F4: No re-entry support at checkpoints
**Severity:** medium
**Lens:** adhd
**Location:** Every 💾 Commit / ✋ Checkpoint block, sub-steps 0-6
**needsRun:** false
**Issue:** Commits are natural save points but none says what the learner has, where the next session resumes, or what the first action is - the contract's "stopping here? you have X working; next session starts at sub-step N, first action: ..." lines are absent throughout.
**Fix:** After each 💾 Commit block add one italic line, e.g. after sub-step 2: "*Stopping here? You have the harness + query-plan lab + all four anomaly tests green and committed. Next session: Sub-step 3 (write skew) - first action: create `WriteSkewTest.java` and re-run `./mvnw -pl services/cif test -Dtest=MvccIsolationTest` to confirm you're still green.*" Repeat with adjusted content for each sub-step.

### F5: No Play-With-It section for queries.sql
**Severity:** medium
**Lens:** structure
**Location:** C - Build, between "The full flow you just built" sequence diagram (~line 938) and "The Finished Result" (~line 960)
**needsRun:** true
**Issue:** The contract's play-with-it movement is missing as a section. `queries.sql` - the step's only hands-on-in-psql artifact - is never walked through; it is only name-checked in the files tree, the cheat card, and one parenthetical. The interactive psql lab promised in the 30-seconds table ("an interactive psql lab") has no guided entry point.
**Fix:** Add "## 🕹️ Play With It (~20 min)" after the sequence diagram: start the throwaway container from the cheat card, open two `psql` sessions, and walk 2-3 experiments from `steps/step-10/queries.sql` (watch `xmin`/`xmax` change across an UPDATE in section 2; reproduce the non-repeatable read by hand with BEGIN in both sessions). Run the experiments for real and paste the actual psql output the learner should see (do not fabricate it).

### F6: Sub-step 0 has no run-and-see
**Severity:** medium
**Lens:** swe
**Location:** Sub-step 0 - DbLab harness, Checkpoint (~line 373)
**needsRun:** true
**Issue:** The checkpoint claims "`DbLab.java` compiles" but gives no command to prove it and no expected output - the only sub-step whose micro-anatomy lacks run-and-see. A typo in the package line or a wrong Testcontainers import surfaces one sub-step later, where it is harder to attribute.
**Fix:** Add a ▶️ Run & See: `./mvnw -pl services/cif test-compile`, run it for real, paste the actual `BUILD SUCCESS` tail, and add one common-wrong-output (e.g. `package com.buildabank.cif.dblab does not exist` when the file is placed under the wrong folder).

### F7: Verification Log entries 9 and 10 lack real pasted output
**Severity:** medium
**Lens:** structure
**Location:** D - Prove, "### 9 - smoke.sh" (~line 1068) and "### 10 - Clean-room (12.4) & chain" (~line 1076)
**needsRun:** true
**Issue:** The tier is 🔴 Full, which promises clean-room fresh-clone plus smoke.sh evidence, but section 9 paraphrases its output ("... (six lab classes, all green) ...") and section 10 pastes nothing at all ("output captured in the build above; reproduced from a pristine clone"). These are claims, not evidence - every other section pastes real output.
**Fix:** Actually run `bash steps/step-10/smoke.sh` and the clean-room fresh-clone (`git clone` to a temp dir at `step-10-end`, `make doctor`, `./mvnw -pl services/cif -am verify`) and paste the genuine tail of each (test counts, BUILD SUCCESS, the smoke PASSED line, and the `git rev-parse step-10-end step-11-start` equality check).

### F8: "Design with read replicas" objective has no aligned checkpoint or retrieval item
**Severity:** medium
**Lens:** pedagogy
**Location:** "What You'll Be Able to Do" bullet 7 (~line 105) vs Test Yourself (~line 1168) and Flashcards (~line 1190)
**needsRun:** false
**Issue:** Replicas/replication lag appear in Understand and in the honest section-7 note, but no checkpoint, your-turn exercise, test-yourself question, or flashcard ever exercises them - the only stated outcome with zero retrieval alignment. Interview Q&A also skips it.
**Fix:** Add Test Yourself question 6: "You write to the primary and immediately read from a replica but don't see your write - name the phenomenon, why async streaming replication causes it, and the SQL to measure lag on primary and replica" (answer: read-your-writes violation / replication lag; `pg_stat_replication` on the primary, `now() - pg_last_xact_replay_timestamp()` on the replica). Optionally swap flashcard 5 for a replication-lag card.

### F9: Dense unbroken stretch in Understand's Under the Hood
**Severity:** low
**Lens:** adhd
**Location:** B - Understand, "Under the Hood" from "Write skew - the subtle one" through "Read replicas & replication lag" (~lines 197-205)
**needsRun:** false
**Issue:** Five consecutive dense paragraphs (write skew ~170 words in a single block, then pools, partitioning, online DDL, replicas) with no visual, code break, or interaction - the worst wall-of-text run in the lesson; the section's only knowledge check sits much earlier (line 178).
**Fix:** Insert a small mermaid sequence diagram of the write-skew interleaving (T1/T2 both read sum=200, debit different rows, both commit, sum=-100) immediately after the write-skew paragraph, and add one ❓ knowledge-check `<details>` after the connection-pool paragraph ("pool size 2, three concurrent borrowers - what happens to the third and after how long?").

### F10: Build bookkeeping - sub-step count label and missing step-10-end tag command
**Severity:** low
**Lens:** structure
**Location:** Sub-step headings ("Sub-step 0 of 6" ... "6 of 6", ~lines 286-870) and Definition of Done (~line 971)
**needsRun:** false
**Issue:** There are seven sub-steps (0-6) but every heading says "of 6", understating remaining work for a learner tracking progress. Separately, the DoD requires "committed and tagged `step-10-end`" but no `git tag` command appears anywhere in the build or Finished Result.
**Fix:** Relabel headings "Sub-step 1 of 7" ... "Sub-step 7 of 7" (renaming the harness from 0 to 1), updating the you-are-here trails to match; and add to "The Finished Result": `git tag step-10-end` (plus the course's tag-push convention if tags are pushed).
