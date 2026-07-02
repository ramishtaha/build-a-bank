# Step 1 audit - swe:8 pedagogy:6 adhd:5 structure:8 - thinBuild:false

## Strengths

- The sacred build is largely honored: 8 sub-steps with near-complete micro-anatomy (goal, location, verbatim code with file-path headers, line-by-line, under-the-hood, predict, run-and-see with common-wrong-output, checkpoint, commit, pitfall), opening Mermaid map + files tree and a closing sequence diagram; the break-it-on-purpose experiment in sub-step 5 is well placed (before the controller exists, so removing the web starter genuinely compiles and exits).
- The TestRestTemplate-removed-in-Boot-4 story is threaded honestly through Then-vs-Now, the actual test code, the troubleshooting section (verbatim compile error), interview Q2, and a flashcard - exemplary version-evolution and misconception handling.
- Strong retrieval package and progress signaling: 6 interview Q&As, 5 test-yourself items, 5 flashcards all targeting the step's core ideas; you-are-here markers on every sub-step header; 7 of 8 sub-steps end in something the learner runs and sees.

## Missing spine

- Sub-step 2 (parent POM): predict-then-run, run-and-see, and commit are missing from the micro-anatomy.
- Sub-step 5 (break-it experiment): under-the-hood, commit, and pitfall are missing; expected behavior is prose, not pasted output.
- Type-it-yourself interactivity is absent lesson-wide (every code block is fully worked, paste-ready).
- Knowledge-checks are absent inside the Build movement (the only one is in Understand).

## Findings

### F1: Title and outcomes promise command line, Linux & Git that the lesson never teaches

**Severity:** high
**Lens:** pedagogy
**Location:** Title (line 1), "This Step in 30 Seconds" effort row (line 35), skip-test (lines 46-47), "What You'll Be Able to Do" bullets 1-2 (lines 95-96)
**needsRun:** true
**Issue:** The step is titled "Setup, the Command Line, Linux & Git" with effort ~20h, and the skip-test/outcomes claim the learner will be able to navigate a filesystem, inspect/kill processes, reason about permissions, and use Git for branch/merge/rebase/conflict/PR work. No section of the lesson teaches any of this - there is no terminal/Linux module, no Git workflow walkthrough, no checkpoint or exercise covering rebase, conflicts, chmod, PIDs, or PRs. Two of six stated outcomes have zero aligned instruction or assessment, and the 20h estimate does not match the ~3h of actual content.
**Fix:** Either (a) add a Build movement B/C module of 2-4 sub-steps teaching terminal basics (pwd/ls/cd/cat/chmod, processes/PIDs with ps/kill) and a hands-on Git arc (init -> branch -> commit -> merge conflict created and resolved -> rebase -> PR concept), each with the full micro-anatomy and real pasted run output; or (b) rescope by retitling the step, deleting skip-test items 1-2 and outcomes bullets 1-2, and lowering the effort estimate to match the Spring Boot content. Option (a) requires real runs for the run-and-see blocks; never invent the output.

### F2: No session plan, save points, or time-boxes for a ~20-hour step

**Severity:** high
**Lens:** adhd
**Location:** "This Step in 30 Seconds" table (lines 29-38) and all sub-step headers
**needsRun:** false
**Issue:** A step estimated at ~20 hours has no planned sittings, no named save points, and no time-boxes per movement or per sub-step - only the single whole-step estimate. An ADHD learner has no way to plan "tonight I do sittings 1-2" or to know whether sub-step 8 is a 10-minute or 2-hour commitment.
**Fix:** Insert a "Suggested sittings" table immediately after the 30-seconds table: 6-8 sittings of 2-3h, each naming its movements/sub-steps, its save point (e.g. "Sitting 3 ends at sub-step 4 checkpoint: Tomcat banner on 8080"), and what will be visibly working. Add a time estimate to every movement heading (e.g. "B - Understand (~45 min)") and to every sub-step header (e.g. "Sub-step 3 of 8 (~20 min)").

### F3: No re-entry support at checkpoints

**Severity:** medium
**Lens:** adhd
**Location:** Every "Checkpoint" block in sub-steps 1-8
**needsRun:** false
**Issue:** Checkpoints confirm state but never support stopping and resuming. A learner returning after three days has no "you have X working; resume at sub-step N; first action is Y" line anywhere.
**Fix:** Append one line to each sub-step checkpoint in the pattern: "Stopping here? You have [state] working. Next session: sub-step N ([name]); first action: [exact command or file to open]." E.g. after sub-step 4: "Stopping here? You have a running (empty) server. Next session: sub-step 6, HelloController; first action: create HelloController.java at the path above."

### F4: Build-along path is not followable from an empty folder; secrets hygiene never exercised

**Severity:** high
**Lens:** swe
**Location:** "Before You Start" two-repo-roles note (lines 110-115), "Your Starting Point" (lines 217-230), Definition of Done last bullet (line 1026)
**needsRun:** true
**Issue:** The lesson says the learner's own empty folder is "the only place Step 1 tells you to run git init" and that they build along by hand - but no sub-step ever runs `git init`, and no sub-step gets the scaffold (mvnw/mvnw.cmd, .mvn/wrapper, parent pom.xml, .gitignore, .env.example) into that folder. The Maven Wrapper cannot be hand-typed, sub-step 3's commit stages a parent pom.xml the learner was never told to create or copy, and the DoD requires ".env is gitignored; only .env.example committed" though no sub-step creates or verifies either file. A nervous beginner at 11pm cannot start the build.
**Fix:** Add "Sub-step 0 - Get the scaffold and initialize Git" with full micro-anatomy: exact commands to copy the step-01-start snapshot (or the listed files) into a fresh folder, `git init`, create `.gitignore` and `.env.example` (contents given verbatim), create `.env`, verify ignoring with `git status` and `git check-ignore -v .env` (paste real output), then the initial commit. Update sub-step 3's git add accordingly. Run the commands to capture truthful output.

### F5: Health-endpoint expected output likely wrong for the shown config (probes groups)

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 7 expected output (line 805), Play With It curl block (line 971), Verification Log item 2 (line 1062)
**needsRun:** true
**Issue:** The expected `/actuator/health` body includes `"groups":["liveness","readiness"]`, but the liveness/readiness health groups are only auto-registered when running on Kubernetes or when `management.endpoint.health.probes.enabled: true` is set - and the application.yml taught in sub-step 7 sets no such key. Either the pasted output did not come from this config or the yml in the lesson is missing a key; as printed, a learner's real output will not match the lesson. The sub-step 7 "expected output" is also paraphrased ("GET /actuator/health -> ...") rather than literal curl output.
**Fix:** Re-run `curl -s http://localhost:8080/actuator/health` against exactly the sub-step 7 config and paste the true body in all three locations. If groups are absent locally, delete `"groups":[...]` from the expected outputs; alternatively add `management.endpoint.health.probes.enabled: true` to the yml with a line-by-line entry explaining probes, then re-run and paste.

### F6: "Real, pasted" verify output has lines in an impossible order

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 8 expected output (lines 902-912) and Verification Log item 1 (lines 1042-1052)
**needsRun:** true
**Issue:** The log shows `[INFO] BUILD SUCCESS` before the reactor-summary line `Build-a-Bank :: Hello Service ... SUCCESS [ 11.473 s]`. Real Maven prints the reactor summary (which in this multi-module build would also include the parent, `Build-a-Bank :: Parent`) first, then BUILD SUCCESS, then Total time. The visible hand-reordering undermines the lesson's own "hard-to-fake evidence" claim.
**Fix:** Re-run `./mvnw -B verify` and paste the genuine tail in true order - reactor summary (parent + hello-service lines), BUILD SUCCESS, Total time - in both the sub-step 8 and Verification Log blocks. Do not reconstruct the order by hand.

### F7: Sub-step 2 breaks the micro-anatomy (no predict, no run-and-see, no commit)

**Severity:** medium
**Lens:** structure
**Location:** Sub-step 2 of 8 - "Understand the parent (aggregator) POM" (lines 327-419)
**needsRun:** true
**Issue:** The contract requires predict-then-run, run-and-see (exact command + expected output), and a commit line for every sub-step. Sub-step 2 has none of the three - the checkpoint is "you understand that..." with nothing runnable, so the learner gets no feedback that the parent POM actually resolves, and the reward loop breaks at the second sub-step.
**Fix:** Add a predict ("What Maven version and how many modules will the root build report?"), a run-and-see of `./mvnw -B validate` at the repo root (or `./mvnw help:effective-pom | head`) with real pasted output, a verifiable checkpoint keyed to that output, and a commit block (or an explicit "nothing to commit - file already present at step-01-start / committed in sub-step 0" line, matching sub-step 1's pattern).

### F8: Sub-step 5 missing commit, pitfall, and pasted output

**Severity:** low
**Lens:** structure
**Location:** Sub-step 5 of 8 - "Break it on purpose" (lines 626-648)
**needsRun:** true
**Issue:** The experiment sub-step has goal/run/checkpoint but no under-the-hood, no commit line, and no pitfall; the outcome ("no Tomcat started line, then exits") is prose, not the exact expected log lines the contract requires. The biggest real risk - forgetting to restore the starter and having every later sub-step fail mysteriously - is unstated as a pitfall.
**Fix:** Paste the real abridged log of the no-web-starter run (Started HelloApplication line, absence of Tomcat line, process exit). Add "Commit: nothing - `git diff` must be empty after restoring" and a pitfall: "If you forget to restore spring-boot-starter-web, sub-step 6 fails to compile (GetMapping/RestController unresolved) - run `git diff services/hello/pom.xml` to confirm it's clean."

### F9: No scaffold fading - all eight sub-steps are fully worked, paste-ready code

**Severity:** medium
**Lens:** pedagogy
**Location:** Whole Build movement (sub-steps 3, 4, 6, 7, 8)
**needsRun:** false
**Issue:** Every code artifact is given "complete, verbatim" with no type-it-yourself moments, so the learner can complete the entire step by copy-paste without ever producing code from memory. The contract's interactivity toolkit expects later sub-steps to shift toward type-it-yourself; here scaffolding never fades.
**Fix:** Convert sub-step 7 (application.yml) or the second test in sub-step 8 to type-it-yourself: give the skeleton with TODO comments naming what each block must do ("bind port 8080", "assert status 200"), keep the full verbatim solution in a collapsed `<details>` block for checking. Frame exercise 1 (the /api/ping endpoint) explicitly as "type it, don't paste".

### F10: No knowledge-checks inside the Build movement

**Severity:** low
**Lens:** pedagogy
**Location:** Build movement (only knowledge-check is in Understand, line 165)
**needsRun:** false
**Issue:** The interactivity contract calls for knowledge-checks sprinkled through the build; the build's only retrieval moments are predicts, which test anticipation, not consolidation.
**Fix:** Add 2-3 collapsed knowledge-checks: after sub-step 3 ("Which dependency's mere presence triggers web auto-configuration, and why do our dependencies carry no versions?"), after sub-step 6 ("What two annotations does @RestController combine?"), after sub-step 8 ("Why RANDOM_PORT instead of 8080 in tests?"), each with a `<details>` answer.

### F11: Optional content lacks time costs

**Severity:** low
**Lens:** adhd
**Location:** "Go Deeper (Optional)" (lines 1083-1101), IntelliJ tip (line 965), "Little experiments" (lines 1009-1012)
**needsRun:** false
**Issue:** Optional blocks carry no time estimates (only the break-it experiment says "30s"), so a time-pressured learner cannot decide whether to engage or defer them.
**Fix:** Add time labels to each optional block header: "Go Deeper - fat jar internals (~10 min)", "Virtual threads (~5 min read)", "In IntelliJ (~5 min, optional)", "Little experiments (~10 min)".

### F12: Sub-step 3 predict asks about a command that is never run

**Severity:** low
**Lens:** pedagogy
**Location:** Sub-step 3 predict (line 496) vs run-and-see (line 501)
**needsRun:** false
**Issue:** The predict asks whether `./mvnw -pl services/hello compile` will succeed, but the run-and-see executes `validate` instead; the prediction is only resolved by a parenthetical in the checkpoint, breaking the predict-then-run feedback loop.
**Fix:** Reword the predict to target the command actually run: "Will `validate` pass with a POM but zero Java sources? What about `compile`?" and keep the checkpoint's parenthetical as the answer to the second half, explicitly labeled "(answer to the predict)".
