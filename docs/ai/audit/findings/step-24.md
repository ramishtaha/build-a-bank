# Step 24 audit - swe:5 pedagogy:4 adhd:3 structure:5 - thinBuild:true

## Strengths

- Honest, evidence-rich Verification Log: Full tier stated, plausible real counts (read 4 / write 2 / filter 1 / skip 1 with matching balance effects), a genuine §12.3 mutation with pasted FAILED output, clean-room, smoke.sh, and a §12.8 honesty note about what was described-not-run.
- Technically accurate and current: Spring Batch 6 package moves, `chunk(size, txManager)` deprecation note, `initialize-schema=never` + Flyway V4, no `@EnableBatchProcessing` — all verified consistent with the actual repo code in `services/demand-account/src/main/java/com/buildabank/account/batch/`.
- Strong orientation and phase-wrap: 30-seconds table with effort/what-to-run, skip-test, cheat card with a headline chunk diagram, explicit dependency callbacks to Steps 8/12/19-22, misconception-busting exactly-once-effect framing, and interview prep that hits the step's marquee questions.

## Missing spine

- Analogy in the Understand big-idea (diagram present, analogy absent)
- What-we-will-build Mermaid diagram at the top of Movement C (only the files tree exists, placed as a "B→C bridge" before the Build heading)
- Closing sequence diagram of the flow built (Build ends with a one-paragraph "Finished Result")
- Per-sub-step micro-anatomy: complete code with file-path headers/imports, line-by-line explanation, under-the-hood, run-and-see with expected output, checkpoint, per-sub-step commit, pitfall — absent for all 3 sub-steps
- In-lesson flashcards (3-5) — Recap (g) only points to `docs/flashcards.md` (a Step 24 section does exist there, line 184)
- Session plan with named save points and re-entry lines (ADHD contract for an ~18-hour step)
- Definition-of-Done rendered as a checklist (currently inline prose in one paragraph)
- `requests.http` absent from `steps/step-24/` (possibly intentional — the job has no HTTP surface — but the lesson never says so)

## Findings

### F1: The build teaches no code at all — every sub-step is a stub

**Severity:** high
**Lens:** structure
**Location:** Movement C, Sub-steps 1-3 (lines 192-206)
**needsRun:** false
**Issue:** The entire build for an 18-hour milestone is ~35 lines. Sub-step 2 compresses five classes (`InterestPosting`, `InterestSkipException`, `InterestProcessor`, `InterestWriter`, `InterestAccrualJobConfig`) plus the fault-tolerance builder chain into one paragraph of inline shorthand (including non-compiling fragments like `skip(InterestSkipException)` without `.class`). Sub-step 3 describes the phase capstone test in three sentences with zero code. A nervous beginner at 11pm cannot produce any of these files from the lesson; every referenced class is unexplained magic.
**Fix:** Rewrite Movement C with full micro-anatomy per sub-step. The finished code already exists in the repo — transcribe it verbatim with file-path header comments and all imports: `services/demand-account/src/main/resources/db/migration/V4__batch_schema.sql` (plus the `application.yml` diff for `spring.batch.job.enabled=false`, `spring.batch.jdbc.initialize-schema=never`, `bank.interest.daily-rate`), the five classes under `services/demand-account/src/main/java/com/buildabank/account/batch/`, and the two tests (`InterestAccrualJobTest`, `PaymentExactlyOnceCapstoneTest`). Add a line-by-line explanation and an under-the-hood note after each block (e.g., why `RepositoryItemReaderBuilder` needs `.sorts(Map.of("id", Sort.Direction.ASC))` for restartable paging; why the writer receives `Chunk<? extends InterestPosting>`).

### F2: No per-sub-step run-and-see — one run command for the whole build

**Severity:** high
**Lens:** swe
**Location:** Sub-steps 1-3 (lines 192-206) and Play With It (lines 208-216)
**needsRun:** true
**Issue:** Sub-step 1 has a single expected log line; sub-steps 2 and 3 have no command, no expected output, and no common-wrong-output at all — the only runnable command appears afterwards in Play With It. The contract requires exact command + expected output + common-wrong-output per sub-step, and the reward loop for ~18 hours of work is currently a single test invocation.
**Fix:** After each sub-step, add a run-and-see block with the exact command and real pasted output: sub-step 1 → run any demand-account test and paste the actual Flyway `Migrating schema "public" to version "4 - batch schema"` lines plus the common-wrong-output (`relation "batch_job_instance" does not exist` when `initialize-schema` is left on); sub-step 2 → `./mvnw -pl services/demand-account test -Dtest=InterestAccrualJobTest` with the real step-count log lines (read/write/filter/skip) and the FAILED output when skip is missing; sub-step 3 → the capstone command with the real duplicate-redelivery/dedupe log lines. Outputs must come from actual runs — do not fabricate them.

### F3: 18-hour milestone with no session plan

**Severity:** high
**Lens:** adhd
**Location:** Movement A, "This Step in 30 Seconds" / "Before You Start" (lines 30-96)
**needsRun:** false
**Issue:** Effort is stated as ≈18 hours focused, but there is no breakdown into sittings, no named save points, and no guidance on where a session can safely end. The contract flags any ~20-hour step without 6-10 planned sittings of ~2-3h.
**Fix:** Add a "Session plan" table to Orient with 6-8 sittings, each 2-3h with a named save point, e.g.: S1 Understand + skip-test (~2h, save: nothing to commit); S2 V4 schema + yml, Flyway migration seen in logs (~2h, save: commit "batch schema"); S3 posting/exception/processor (~2.5h); S4 writer + job config, compile green (~2.5h); S5 `InterestAccrualJobTest` green + experiments (~3h); S6 capstone test green (~3h); S7 mutation + smoke.sh + clean-room + tag (~2h).

### F4: No checkpoints, no re-entry lines, one commit for the whole step

**Severity:** high
**Lens:** adhd
**Location:** Movement C — the single commit line at line 206
**needsRun:** false
**Issue:** The only commit is after sub-step 3; there are no checkpoint boxes and no "stopping here?" re-entry support anywhere. A learner interrupted mid-build (likely, over 18 hours) has no defined resume point, and losing sub-step 2 work means losing most of a day.
**Fix:** End each sub-step with (a) a checkpoint line ("You now have: Flyway V4 applied / the job COMPLETES with read 4, write 2..."), (b) a per-sub-step commit (e.g., `feat(demand-account): batch JobRepository schema via Flyway V4`), and (c) a re-entry line: "Stopping here? You have X working; next session starts at Sub-step N — first action: open <file> / run <command>."

### F5: Sub-step 2 is a working-memory bomb — five classes in one paragraph

**Severity:** medium
**Lens:** adhd
**Location:** "Sub-step 2 — the interest-accrual job" (lines 196-200)
**needsRun:** false
**Issue:** One paragraph introduces the processor contract, null-filtering, a skip sentinel, the writer's lock-and-credit loop, `RepositoryItemReader`, chunk size, `.faultTolerant()`, `skipLimit`, and `retryLimit` — far beyond ~3 new terms per sub-step, with no visual break and nothing runnable in between.
**Fix:** Split Sub-step 2 into four sub-steps, one idea each: 2a `InterestPosting` + `InterestSkipException` (the data + the sentinel), 2b `InterestProcessor` (compute / filter / skip — one concept per branch), 2c `InterestWriter` (re-read with the Step-12 lock, credit, ledger entry), 2d `InterestAccrualJobConfig` (reader, chunk, then the fault-tolerance chain as its own explained block). Renumber the capstone to Sub-step 3→6 and add "Sub-step X of 6" you-are-here markers.

### F6: Build lacks its opening what-we-will-build diagram and closing sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** Movement C open (line 186) and "The Finished Result" (lines 218-220)
**needsRun:** false
**Issue:** The contract requires the build to open with a what-we-will-build Mermaid diagram + files tree and close with a sequence diagram of the flow built. Only the files tree exists (placed before the Build heading as a "B→C bridge"); there is no closing sequence diagram for either the batch flow or the capstone flow.
**Fix:** Move the files tree under the Movement C heading and add above it a Mermaid flowchart showing both deliverables (accounts table → reader → processor → writer → ledger, with the JobRepository at the side; plus the capstone lane: transfer + Idempotency-Key → outbox → relay → Kafka → consumer dedupe). Close "The Finished Result" with a Mermaid `sequenceDiagram`: JobLauncher → Step → reader/processor/writer per chunk → JobRepository counts; then the capstone payment with the forced duplicate and the eventId dedupe branch.

### F7: Flashcards not in the lesson

**Severity:** medium
**Lens:** structure
**Location:** Recap item (g) (line 322)
**needsRun:** false
**Issue:** The contract requires 3-5 flashcards in the recap; the lesson only says they were "appended to `docs/flashcards.md`". (A Step 24 section does exist there, but the lesson itself must carry them.)
**Fix:** Inline 4 Q/A flashcards at (g): (1) What is committed in a chunk-oriented step? → each chunk of N items in one transaction; (2) skip vs retry vs filter? → skip = tolerate a bad record up to a limit, retry = re-attempt a transient failure, filter = processor returns null; (3) What makes a Batch job restartable? → the JobRepository's JobInstance/JobExecution records keyed by identifying JobParameters; (4) Exactly-once delivery vs effect? → delivery is impossible; effect = at-least-once + idempotent consumer + Outbox.

### F8: Retried exception doesn't match the described lock conflict

**Severity:** medium
**Lens:** swe
**Location:** "Security Lens & Thread-safety note" (lines 143-147) vs Sub-step 2 (line 198)
**needsRun:** false
**Issue:** The Security Lens says the writer re-reads each account with the Step-12 **pessimistic** lock "and a lock conflict is the transient we retry", but the configured retry (lesson and repo code) is `OptimisticLockingFailureException`. A pessimistic lock timeout raises `PessimisticLockingFailureException` — a sibling, not a subclass — so the described conflict would NOT be retried; conversely, holding PESSIMISTIC_WRITE from re-read to commit largely prevents the `@Version` optimistic failure. The prose and the config tell contradictory stories.
**Fix:** Correct the Security Lens prose: the retry covers the `@Version` optimistic conflict (Step 9) that can surface when the batch write races a live transfer outside the locked window; note explicitly that a pessimistic lock timeout would surface as `PessimisticLockingFailureException`, and that widening the retry to their common parent `ConcurrencyFailureException` covers both (flag that changing the code would require re-running the tests).

### F9: Interactivity toolkit nearly absent; the break-it-on-purpose is done for the learner

**Severity:** medium
**Lens:** pedagogy
**Location:** Movement C (one predict at line 200); §12.3 mutation lives only in the Verification Log (lines 243-250)
**needsRun:** false
**Issue:** One predict-then-run in the whole build; no type-it-yourself, no knowledge-checks, and the skip-removal mutation — the step's perfect break-it-on-purpose — is performed by the author in Movement D instead of by the learner, so scaffolding never fades.
**Fix:** In sub-step 2d, add a break-it-on-purpose box: comment out `.skip(InterestSkipException.class).skipLimit(100)`, predict the job status, run `InterestAccrualJobTest`, compare with the FAILED output already pasted in Movement D, restore. Make the writer (2c) type-it-yourself with a signature-only scaffold. Add two knowledge-checks: after 2b ("what happens to an account whose processor returns null — written, skipped, or filtered?") and before sub-step 3 ("the relay publishes then marks — which duplicate does that gap allow?").

### F10: No time-boxes below the whole-step level

**Severity:** medium
**Lens:** adhd
**Location:** All movement and sub-step headings
**needsRun:** false
**Issue:** The only time information is the global "≈ 18 hours". No movement or sub-step carries an estimate, and the two Go-Deeper collapsibles have no time cost, so a learner cannot plan a sitting or decide whether to open optional content.
**Fix:** Add estimates to each heading: B ~2h; sub-step 1 ~2h; sub-step 2 (or 2a-2d) ~7h total, broken down; sub-step 3 ~4h; D ~2.5h; E ~1.5h; F ~1h. Label each Go-Deeper collapsible "(~10 min read, optional)".

### F11: Big Idea has no analogy

**Severity:** low
**Lens:** pedagogy
**Location:** "The Big Idea" (lines 104-119)
**needsRun:** false
**Issue:** The contract's Understand movement is big idea + diagram + analogy; the diagram is there, the analogy is not — the concept goes straight from definition to Mermaid.
**Fix:** Add a short analogy paragraph after the Mermaid, e.g.: a night-shift clerk works a stack of paper accounts in bundles of ten, stamping the register after each bundle — if the fire alarm interrupts, tomorrow's clerk resumes at the last stamp (JobRepository/restart); a torn page goes in the problem tray to reconcile later (skip) instead of stopping the night; a page someone else is holding gets retried in a minute (retry); a page with nothing owed is set aside (filter).

### F12: requests.http absent with no in-lesson explanation

**Severity:** low
**Lens:** structure
**Location:** `steps/step-24/` folder (only `lesson.md` and `smoke.sh`); Play With It (lines 208-216)
**needsRun:** false
**Issue:** The step folder ships no `requests.http`. The lesson implies why ("launched by a JobLauncher (no HTTP surface)") but never states that the usual file is intentionally absent, which will read as an omission to learners who expect it from every prior step.
**Fix:** Add one line to Play With It: "No `requests.http` this step — the batch job has no HTTP surface; the capstone's transfer requests are exercised inside `PaymentExactlyOnceCapstoneTest`." (Or ship a minimal `requests.http` with the capstone's idempotent transfer call against demand-account.)
