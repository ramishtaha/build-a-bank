# Step 12 audit - swe:6 pedagogy:7 adhd:4 structure:7 - thinBuild:true

## Strengths

- Technically rigorous and honest concurrency capstone: the lost update is made deterministic with a `CyclicBarrier`, and the §12.3 mutation log candidly distinguishes "mass optimistic-lock rejection" from "lost money" (noting `@Version` still preserved conservation) instead of overclaiming.
- Outstanding Phase-B convergence: explicit, accurate callbacks to Steps 7-11 woven through Understand/Build, plus a cumulative Phase-B review quiz (lines 826-836) that is genuinely distinct from the per-step Test Yourself.
- Complete six-movement spine with resolving TOC anchors, you-are-here markers on every sub-step header, opening flowchart + files tree and closing sequence diagram, a Definition-of-Done checklist, 5 flashcards, and strong interview/STAR framing.

## Missing spine

- run-and-see (exact command + expected output + common-wrong-output) absent from sub-steps 0-4; sub-step 5 gives commands but no expected-output block; no sub-step has a common-wrong-output
- predict-then-run absent from sub-steps 0, 2, and 5
- complete-code requirement: no Java snippet has a package line, imports, or file-path header comment; most of the service's files are never shown at all ("full file in the repo")
- type-it-yourself: none anywhere in the build (all shown code is fully worked)
- per-movement and per-sub-step time-boxes absent (only the whole-step ~22h estimate)
- session plan with named save points absent (required for a ~20h+ step)
- re-entry lines at checkpoints ("stopping here? next session starts at...") absent

## Findings

### F1: Build is not completable from the lesson - most files deferred to "full file in the repo"

**Severity:** high
**Lens:** swe
**Location:** C · Build, sub-steps 0-6 (lines 257, 313, 333, 396, 490, 545)
**needsRun:** false
**Issue:** The build systematically shows fragments and defers the rest: pom.xml ("mirrors cif's"), application.yml, compose.yaml, `LedgerEntry`, `AuditEntry`, `EntryDirection`, `InsufficientFundsException`, `Account`'s constructor/`requirePositive`, `TransferService.openAccount/balanceOf/totalSystemBalance/ledgerNet`, the full `PropagationDemoService`, all DTOs, `ApiExceptionHandler`, `ContainersConfig`, and 5 of the 6 test classes are never shown. Checkpoints then demand these exist (e.g. line 346: "`LedgerEntry`, `AuditEntry`, `EntryDirection`, `InsufficientFundsException` all exist"). No shown Java snippet has a package line, imports, or file-path header comment. A nervous beginner at 11pm cannot build this service top-to-bottom from the lesson.
**Fix:** Paste the complete, compiling file (file-path header comment + package + imports) for every file each checkpoint requires, in the sub-step that introduces it: sub-step 0 gets pom.xml, application.yml, compose.yaml; sub-step 1 gets full Account, LedgerEntry, AuditEntry, EntryDirection, InsufficientFundsException; sub-step 3 gets the full TransferService (openAccount, balanceOf, totalSystemBalance, ledgerNet); sub-step 4 the full PropagationDemoService; sub-step 5 the DTO records + ApiExceptionHandler; sub-step 6 ContainersConfig and the other 5 test classes (long ones may go in a collapsed `<details>`). Copy the code verbatim from services/demand-account (already verified) and add package/imports to the snippets already shown.

### F2: run-and-see missing from sub-steps 0-4; sub-step 5 has no expected output

**Severity:** high
**Lens:** structure
**Location:** Sub-steps 0-4 (lines 261-480) and sub-step 5's Run & See (lines 520-531)
**needsRun:** true
**Issue:** The micro-anatomy contract requires run-and-see (exact command + expected output + common-wrong-output) for every sub-step. Sub-steps 0-4 end at "compiles"-style checkpoints (sub-steps 1-4 don't even state the compile command); sub-step 5 gives curl commands but no expected-output block; no sub-step anywhere has a common-wrong-output. Only sub-step 6 has a real run-and-see.
**Fix:** Add a run-and-see block per sub-step with real captured output: sub-step 0 `./mvnw -q -pl services/demand-account -am compile` (paste BUILD SUCCESS tail); sub-steps 1-2 the same compile plus a targeted `-Dtest=` run of one small test written alongside; sub-step 3 `./mvnw -pl services/demand-account test -Dtest=TransferServiceTest` with pasted surefire summary; sub-step 4 `-Dtest=TransactionPropagationTest`; sub-step 5 the pasted JSON bodies + status codes of the three curls. Add one common-wrong-output per block (e.g. the Flyway checksum error, the "Could not find a valid Docker environment" error). All output must come from actually running the commands.

### F3: No session plan for a ~22-hour step

**Severity:** high
**Lens:** adhd
**Location:** A · Orient, "This Step in 30 Seconds" table (line 35) / Before You Start (line 108)
**needsRun:** false
**Issue:** The step is estimated at ~22 hours but there is no sitting plan anywhere - an ADHD learner faces one undifferentiated multi-day wall. The contract for ~20h steps requires 6-10 planned sittings of ~2-3h with named save points.
**Fix:** Add a "Suggested sittings" table to Orient, e.g.: S1 Orient+Understand (~2.5h, save point: skip-test answered); S2 sub-steps 0-1 module/schema/entities (~3h, save: sub-step 1 commit); S3 sub-step 2-3 repo+TransferService (~3h, save: sub-step 3 commit); S4 sub-step 4 propagation (~2h); S5 sub-step 5 REST + live run (~2.5h); S6 sub-step 6 tests + capstone (~3h); S7 Prove: mutation + smoke + clean-room (~2.5h); S8 Apply+Review (~2h). Each save point is an existing commit line.

### F4: No re-entry support and no per-sub-step/per-movement time-boxes

**Severity:** medium
**Lens:** adhd
**Location:** Every sub-step's commit line (lines 301, 348, 384, 438, 478, 533, 606); movement headings A-F
**needsRun:** false
**Issue:** Nothing tells a returning learner where they are or how long the next chunk takes. Commits are natural stopping points but carry no "stopping here? you have X working; next session starts at sub-step N, first action: ..." line, and neither movements nor sub-steps carry time estimates (only the whole-step ~22h).
**Fix:** After each sub-step's commit add one italic re-entry line, e.g. after sub-step 3: "*Stopping here? You have a deadlock-safe, transactional transfer committed. Next session: sub-step 4, first action: create `service/AuditService.java`.*" Add "⏱ ~Xh" to each sub-step heading and each movement heading (A ~1h, B ~2.5h, C ~13h split per sub-step, D ~2.5h, E ~2h, F ~1h).

### F5: First visible win comes many hours in; reward loop is compile-only for sub-steps 0-4

**Severity:** medium
**Lens:** adhd
**Location:** Sub-steps 0-4 (lines 261-480); first live run is sub-step 5 (line 520)
**needsRun:** true
**Issue:** The learner writes schema, entities, repository, service, and propagation demo - plausibly 8-12 hours - before anything visibly runs; every intermediate checkpoint is "it compiles". The contract wants something visibly running within the first ~10 minutes of the build.
**Fix:** At the end of sub-step 0, add a first-win run: `docker compose -f services/demand-account/compose.yaml up -d` then boot the (empty) app and hit `GET localhost:8082/actuator/flyway` to see V1 applied - paste the real JSON. Then give each of sub-steps 1-4 one runnable feedback moment (a small targeted test run, per F2) so the learner sees green output at least once per sitting.

### F6: transferUnsafe/applyBalanceUnsafe - the capstone's broken path - is never shown

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 2 under-the-hood (line 380), sub-step 3 checkpoint (line 436), capstone test (lines 547-561)
**needsRun:** false
**Issue:** The capstone test calls `transfers.transferUnsafe("ACC-A","ACC-B", amount, "race-1", afterRead)` - a 5-arg method with an injected `Runnable` hook - and the mechanism that makes the lost update real (a bulk absolute-write query that bypasses `@Version`, with the barrier hook between read and write) is only alluded to in one parenthetical. The learner cannot write it, and cannot understand why the lesson's own headline demo works; the lesson's contrast (safe vs unsafe path) rests on unexplained magic.
**Fix:** Add the full `transferUnsafe` + `applyBalanceUnsafe` code (copied from the repo) to sub-step 3 as a clearly-marked "the deliberately broken twin" block, with a line-by-line explaining (a) why a bulk `UPDATE account SET balance = :b` bypasses both dirty-checking and `@Version`, and (b) how the injected `afterRead` Runnable lets the test force both threads to read before either writes.

### F7: Sub-step 6 expected output contains log lines the shown test code never prints

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 6, Run & See expected output (lines 595-600) vs. the pasted test code (lines 546-583)
**needsRun:** false
**Issue:** The expected output shows `[capstone:no-lock] A=100.0000 B=100.0000` and `[capstone:pessimistic] failures=0 ...`, but the test code shown contains no print statements - a learner who types exactly what is shown gets output that doesn't match the "expected output", undermining trust at the most important moment of the step.
**Fix:** Paste the repo's actual test code including its `System.out.printf`/logging lines into the shown snippet (or, if the repo tests don't print, trim the expected output to only the surefire `Tests run: 2, Failures: 0...` lines). Shown code and shown output must be produced by each other.

### F8: Live-run commands are bash-only; Windows learners will fail at sub-step 5 and Play With It

**Severity:** medium
**Lens:** swe
**Location:** Cheat Card (lines 61-76), sub-step 5 Run & See (lines 520-528), Play With It (line 633)
**needsRun:** false
**Issue:** `SPRING_DATASOURCE_URL=jdbc:... ./mvnw ...` uses the bash inline-env-var prefix, and the curl examples use single-quoted JSON - both fail in PowerShell/cmd. The lesson's only Windows note is "Windows uses `.\mvnw.cmd`", which is insufficient for these commands (and the course repo itself lives on a Windows machine).
**Fix:** Beside each live-run command add the PowerShell form: `$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5433/demand_account'; .\mvnw.cmd -pl services/demand-account spring-boot:run`, and point Windows users at `steps/step-12/requests.http` instead of the quoted-JSON curls (or give double-quote-escaped curl variants).

### F9: Verification Log sections 2, 5, 6, 8 are prose summaries, not pasted output

**Severity:** medium
**Lens:** structure
**Location:** D · Prove, sections 2 (line 668), 5 (line 689), 6 (line 692), 8 (line 701)
**needsRun:** true
**Issue:** The contract requires real pasted output in the Verification Log. Sections 1, 3, 4, and 7 paste output, but section 2 (ledger behaviour), 5 (optimistic lock + propagation), 6 (live HTTP round-trip), and 8 (clean-room) only describe results in prose - the reader cannot distinguish verified evidence from narrative.
**Fix:** Run the named tests/commands and paste the actual evidence: the surefire per-class lines for `TransferServiceTest`, `OptimisticLockTest`, `TransactionPropagationTest`, the key assertion/log lines from `DemandAccountIntegrationTest` (or its passing surefire line), and the tail of the clean-room `./mvnw verify` (module reactor summary + BUILD SUCCESS).

### F10: No type-it-yourself anywhere - scaffolding never fades

**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, sub-steps 4-5 (lines 444-535)
**needsRun:** false
**Issue:** Every line of code the learner touches is either fully worked or omitted entirely; the interactivity toolkit's type-it-yourself element is absent and later sub-steps do not shift responsibility to the learner, so a 22-hour step ends with zero independent construction before the Your Turn section.
**Fix:** Convert two late, low-risk files into guided type-it-yourself: sub-step 4's `PropagationDemoService` ("write a @Transactional method that calls auditService.record then throws IllegalStateException - solution below") and sub-step 5's DTO records ("write OpenAccountRequest/TransferRequest as records with @NotBlank/@Positive - solution below"), each with the full solution in a collapsed `<details>`. Keep sub-steps 0-3 fully worked.

### F11: Wall-of-text offenders in Understand

**Severity:** low
**Lens:** adhd
**Location:** "Under the Hood: How It Really Works" opening paragraph (line 172) and "Thread-safety note" (line 214)
**needsRun:** false
**Issue:** Both are ~150-170-word single paragraphs packing multiple distinct ideas (proxy mechanics + self-invocation + ThreadLocal binding; three defence layers + distributed reasoning) with no visual, code, or list break - the densest working-memory load in the lesson.
**Fix:** Break each into a short lead sentence plus a numbered/bulleted list (the text already enumerates "(1)/(2)" and "(1)/(2)/(3)" - promote those to real list items with bolded lead-ins). No content changes needed.
