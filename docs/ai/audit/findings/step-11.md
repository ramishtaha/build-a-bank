# Step 11 audit - swe:5 pedagogy:7 adhd:4 structure:7 - thinBuild:true

## Strengths

- Technically accurate and honest concurrency content: JMM/happens-before/CAS/LongAdder/virtual-thread explanations are correct, version claims are properly hedged (JEP 444 stable since 21, pinning "verify for your JDK", structured concurrency flagged as preview), and the deterministic CyclicBarrier lost-update design plus the never-flaky `<= EXPECTED` assertion is genuinely excellent test craftsmanship.
- Strong cross-step coherence: Step 9 `@Version` / Step 10 MVCC / Step 11 `synchronized`+CAS are explicitly framed as the same lost-update problem at three layers, with a concrete forward hook into Step 12's money ledger.
- The Prove movement includes a real mutation sanity-check (remove `synchronized`, pasted `145225L` failure output, revert), and interview prep / test-yourself / flashcards all target the step's actual core ideas rather than trivia.

## Missing spine

- C Build: Play-with-it section (build jumps from sub-step 3 straight to The Finished Result).
- C Build: run-and-see (exact command + expected output) missing entirely in sub-step 0 and reduced to a bare compile command with no output in sub-step 1.
- C Build: common-wrong-output block absent from every run-and-see in the lesson.
- C Build: predict-then-run missing in sub-steps 0, 1, and 3 (only sub-step 2 has one).
- C Build: under-the-hood missing in sub-step 0; pitfall missing in sub-step 1.
- D Prove section 4: no pasted output — full-repo `./mvnw verify` and the clean-room check are asserted in prose only.
- Step folder: `requests.http` absent (smoke.sh present; step has no HTTP surface, so likely acceptable — confirm fleet convention for pure-JVM steps).

## Findings

### F1: Code is not complete or compilable as shown in any sub-step

**Severity:** high
**Lens:** swe
**Location:** C Build, all four sub-steps (lines 272-345, 363-388, 424-452)
**needsRun:** false
**Issue:** The sacred-build contract requires complete code (file-path header, package line, all imports, compiles as shown). Every sub-step violates it: sub-step 0's pom opens with a literal `<project xmlns="..." ...>` ellipsis; sub-step 1's five classes have no `package com.buildabank.concurrency;` line and no imports (two classes dodge imports via fully-qualified names inline); sub-step 2 shows test methods with no class declaration or imports and calls `awaitQuietly(...)`, `hammer(...)`, and `EXPECTED` which are never defined anywhere in the lesson; the lesson repeatedly says "full file in the repo." A nervous beginner at 11pm cannot type this build in top-to-bottom without outside help, and the shown test code contains no print statements yet the expected output includes `[race] ...` lines.
**Fix:** Paste the complete verified files from the repo (they exist at `playground/concurrency-lab/src/...`): full `pom.xml` with real xmlns attributes; each class with package line, imports, and file-path header comment; `LostUpdateRaceTest.java` in full including the `awaitQuietly` helper, the `hammer` method, the `EXPECTED` constant, and the `System.out.printf` lines that produce the `[race]` output shown in Run & See. Delete every "full file in the repo" deferral.

### F2: The third ConcurrencyToolsTest test is never shown, so the build as typed cannot match the claimed output

**Severity:** high
**Lens:** swe
**Location:** Sub-step 3 of 3 (lines 418-471) and its Run & See / checkpoint
**needsRun:** false
**Issue:** The goal, the 30-seconds table, and the outcomes all promise `CompletableFuture`, and the expected output says `Tests run: 3` (checkpoint: "8 tests total"), but only two tests (virtual threads, Semaphore) are shown. The repo's `ConcurrencyToolsTest.java` has 3 `@Test` methods. A learner typing exactly what the lesson shows gets `Tests run: 2`, fails the checkpoint and the Definition of Done, and the "Use CompletableFuture" objective has no built artifact in the lesson at all (only stretch exercise 4).
**Fix:** Paste the missing CompletableFuture test from the repo file into sub-step 3 as complete code, add a line-by-line entry for it (`thenCombine`/`allOf` semantics), and a predict-then-run ("will the combined future finish before or after both inputs?").

### F3: 20-hour step has no session plan, no re-entry support, and no time-boxes below the whole-step level

**Severity:** high
**Lens:** adhd
**Location:** Whole lesson; Orient table (line 36) states "≈ 20 hours focused"
**needsRun:** false
**Issue:** The contract requires a ~20h step to be split into 6-10 named sittings of ~2-3h with save points. There is none: no session plan, no per-movement or per-sub-step time estimates (only "30s" on break-it and "5-minute" on the skip test), and no re-entry lines at checkpoints ("stopping here? you have X working; next session starts at sub-step N, first action: ..."). An ADHD learner facing an undifferentiated 20-hour block has no way to plan or resume. Go Deeper items also carry no time cost.
**Fix:** Add a "Session plan" box at the end of Orient splitting the step into ~7 sittings (e.g., S1 Orient+Big Idea 2h; S2 Under-the-Hood+Pattern Spotlight 2.5h; S3 sub-steps 0-1 2.5h; S4 sub-step 2 3h; S5 sub-step 3 + Prove 2.5h; S6 Apply 3h; S7 Review+exercises 2h), each ending at a named save point (a commit). Add "⏱ ~Xh" to each movement heading and each sub-step heading. After each 💾 Commit line add a one-line re-entry note: "Stopping here? You have <artifact> committed; next session opens at Sub-step N — first action: `<command>`." Label each Go Deeper item with a time cost.

### F4: "No Docker" promise contradicted by the starting-point verification command

**Severity:** medium
**Lens:** swe
**Location:** "Your Starting Point" (lines 232-235) vs the 30-seconds table (line 37) and Prove intro (line 510)
**needsRun:** false
**Issue:** The step promises "JVM + Maven only — no Docker, no database" three times, but the very first command of the Build is `./mvnw -q -pl services/cif -am verify`, which runs cif's Testcontainers tests and therefore requires Docker (the Verification Log itself says "cif on Testcontainers"). A learner who took the promise literally and shut Docker down gets a wall of red as their first experience of the step, and even with Docker up it is a slow first action (hurts first-win-fast).
**Fix:** Replace the confirm-start command with a Docker-free fast check, e.g. `git describe --tags` (confirm `step-11-start`) plus `./mvnw -q -pl services/cif -am verify -DskipTests` or `./mvnw -q validate`, and add one sentence: "the full `./mvnw verify` (which needs Docker for cif) is deferred to the Prove movement."

### F5: Verification Log section 4 asserts full-repo verify and clean-room with no pasted output

**Severity:** medium
**Lens:** structure
**Location:** D Prove, "4 · Full-repo `./mvnw verify` (all 6 modules) & clean-room (§12.4)" (lines 541-542)
**needsRun:** true
**Issue:** The contract requires real pasted output in the Verification Log. Sections 1, 3, and 5 paste output, but section 4 — the full-tier claims (`BUILD SUCCESS` across all modules, fresh-clone reproduction, `step-11-end == step-12-start`) — is prose only. For a step whose tier is stated as 🔴 Full, the widest-scope evidence is the one part with no evidence.
**Fix:** At the `step-11-end` tag, actually run `./mvnw verify` and paste the reactor summary block (module list + BUILD SUCCESS + total time); run the clean-room clone + verify and paste its final lines. Do not hand-write these blocks.

### F6: Interactivity toolkit is sparse and scaffolding never fades

**Severity:** medium
**Lens:** pedagogy
**Location:** C Build, sub-steps 0, 1, and 3
**needsRun:** false
**Issue:** Across the whole build there is exactly one predict-then-run (sub-step 2) and one break-it-on-purpose (sub-step 2); there are zero knowledge-checks in the Build, and no type-it-yourself progression — every sub-step is a fully worked example (or an excerpt), so the scaffold never fades. The contract expects these sprinkled through the build and later sub-steps shifting toward learner-produced code.
**Fix:** Add (1) a predict before sub-step 1's compile checkpoint ("if you drop `synchronized` from `get()`, which JMM property breaks — atomicity or visibility?"); (2) a knowledge-check box after sub-step 1 (match each of the four implementations to lock / CAS / striping / broken); (3) convert sub-step 3's Semaphore test into guided type-it-yourself: give the test skeleton and assertion, have the learner write the acquire/increment/max/release body from the invariant description, with the full solution in a collapsed `<details>`.

### F7: Build has no Play-with-it section

**Severity:** medium
**Lens:** structure
**Location:** C Build, between sub-step 3 (line 471) and "The full flow you just built" (line 475)
**needsRun:** false
**Issue:** The contract's Build movement requires a play-with-it section before the finished result. The lesson jumps from the last commit straight to the closing sequence diagram and Definition of Done. The raw material exists (the Your Turn exercises) but nothing invites low-stakes experimentation inside the build itself.
**Fix:** Insert a "🎮 Play with it (10-15 min)" section after sub-step 3 with 2-3 prompts that reuse the just-built code, e.g.: crank `DEPOSITS_PER_THREAD` to 1,000,000 and watch the loss grow; change `CyclicBarrier(2)` to `(3)` and predict the hang (then Ctrl-C and explain why); set `Semaphore` permits to 1 and observe `maxObserved`. Each prompt phrased as predict → run → observe, no new files.

### F8: Under-the-Hood is a ~900-word wall of text with no visual or interactive break

**Severity:** medium
**Lens:** adhd
**Location:** B Understand, "🌱 Under the Hood" (lines 177-197)
**needsRun:** false
**Issue:** Eight consecutive dense paragraphs cover bytecode, word tearing, monitors, volatile, CAS vs striping, virtual threads, AutoCloseable executors, and four classic bugs — the virtual-threads paragraph alone is ~200 words, and the AtomicLong-vs-LongAdder paragraph ~150, with no diagram, code block, or check between them. This is the heaviest working-memory stretch of the lesson and the worst overwhelm risk.
**Fix:** Break it with three inserts: a small Mermaid diagram of virtual-thread mount/unmount on carrier threads (after the virtual-threads paragraph); a 3-column mini-table `synchronized` / `AtomicLong` / `LongAdder` (mechanism, best contention level, cost) replacing part of the CAS-vs-striping paragraph; and one knowledge-check (`volatileCounter++` — safe or not?) midway. Move the word-tearing JLS subtlety into a collapsed `<details>` labeled "optional, 2 min".

### F9: run-and-see missing for sub-steps 0-1 and no common-wrong-output anywhere

**Severity:** medium
**Lens:** structure
**Location:** Sub-step 0 (lines 266-299), sub-step 1 checkpoint (line 351), and both Run & See blocks (lines 397-406, 456-465)
**needsRun:** true
**Issue:** Sub-step 0 has no run-and-see at all (first visible result arrives only after typing the pom plus five Java files), sub-step 1's checkpoint gives a compile command but no expected-output block, and no run-and-see in the lesson has the contract-required common-wrong-output. The troubleshooting table partially compensates but is 300 lines away from the point of failure.
**Fix:** End sub-step 0 with `./mvnw -q -pl playground/concurrency-lab validate` and its real output; add the expected tail of the compile to sub-step 1's checkpoint; add a common-wrong-output block to each run-and-see produced by actually inducing the failure (e.g. omit the `<module>` line in the root pom and paste the "Child module ... does not exist" error; run sub-step 2 with `UnsafeBalance` in an exact test and paste the assertion failure — the Prove §3 mutation output can seed this one).

### F10: Sub-step numbering says "of 3" for four sub-steps

**Severity:** low
**Lens:** structure
**Location:** Sub-step headings at lines 266, 303, 357, 418
**needsRun:** false
**Issue:** The build has four sub-steps labeled "Sub-step 0 of 3" through "Sub-step 3 of 3". Zero-indexed "N of 3" reads as three total and undermines the visible-progress marker it's meant to provide.
**Fix:** Renumber to "Sub-step 1 of 4" ... "Sub-step 4 of 4" (keep the you-are-here trail text), and update the build-overview Mermaid node labels (0-3) to match.

### F11: Module count inconsistency — "all 6 modules" enumerates five

**Severity:** low
**Lens:** swe
**Location:** "Your Starting Point" (line 230) and Prove §4 (line 542)
**needsRun:** false
**Issue:** Line 230 says the repo builds with 5 modules and this step adds a 6th; Prove §4 then says "`./mvnw verify` builds and tests every module (hello, cif on Testcontainers, java-basics, spring-lab, concurrency-lab)" — only five names for "all 6 modules" (`libs/common` is missing from the parenthetical, per the root pom's modules block).
**Fix:** In Prove §4 change the parenthetical to enumerate all six: "common, hello, cif (Testcontainers), java-basics, spring-lab, concurrency-lab".
