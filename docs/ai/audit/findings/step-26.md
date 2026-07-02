# Step 26 audit - swe:5 pedagogy:4 adhd:2 structure:5 - thinBuild:true

## Strengths

- The Understand movement is technically accurate and well-framed: correct hexagonal/DDD-tactical vocabulary, the dependency rule stated crisply, proportionate-DDD guidance ("no aggregates in a thin read context"), and an honest §12.8 note that package-based boundaries are convention until ArchUnit (Step 27).
- The behaviour-preservation proof strategy is excellent: "assertions unchanged, only imports moved" as the contract, plus a §12.3 mutation check demonstrating the new application use case is actually on the exactly-once hot path.
- Strong step linking: Step 25 (DIP/first port) grows into the hexagon, Steps 20/21 artifacts are explicitly named as the pieces becoming adapters, and Step 27 (ArchUnit enforcement) is set up repeatedly.

## Missing spine

- Build sub-step micro-anatomy for ALL 4 sub-steps: no complete code (zero code blocks in the entire Build), no file-path-header snippets, no line-by-line explanation, no per-sub-step under-the-hood, no run-and-see (exact command + expected output + common-wrong-output), no checkpoints, no per-sub-step commits (single commit at the end), only one pitfall.
- Closing sequence diagram of the flow built (build ends with prose only).
- Inline flashcards (3-5) — recap item (g) only points at `docs/flashcards.md`.
- Session plan (a ~12h step needs ~4-6 sittings with named save points) and time-boxes per movement/sub-step.
- Re-entry support lines and you-are-here / "sub-step X of N" markers.
- Analogy in the Big Idea (diagram present, analogy absent).
- Interactivity toolkit in the build: only one predict-then-run; no type-it-yourself, no learner-facing break-it-on-purpose, no knowledge-checks.
- `requests.http` absent from `steps/step-26/` (smoke.sh present).

## Findings

### F1: Build has zero code — sub-steps are stubs

**Severity:** high
**Lens:** structure
**Location:** "C · Let's Build It", Sub-steps 1-3 (lines 198-212)
**needsRun:** false
**Issue:** Each sub-step is 2-5 lines of goal prose. The learner is told to "carve out the domain" and create `NotifyOnTransfer`, `NotificationPublisher`, and a refactored `NotificationService`, but is never shown a single line of code: no port interface source, no refactored use case, no moved class with its new `package` line, no adapter diff. A nervous beginner at 11pm cannot complete this restructure from the lesson alone — this violates the sacred-build contract for every sub-step.
**Fix:** Rewrite Sub-steps 1-3 with full micro-anatomy, lifting the real code from the repo (it exists at tag `step-26-end`; the layout is live under `services/notification/src/main/java/com/buildabank/notification/{domain,application,adapter}`). For each sub-step: Goal; exact file path (e.g. `services/notification/src/main/java/com/buildabank/notification/application/port/in/NotifyOnTransfer.java`); complete code with file-path header comment, package line, and all imports (new files: `NotifyOnTransfer`, `NotificationPublisher`, moved `ProcessedEventStore`, `NotificationService`, moved `TransferEvent`/`Notification`; diff view for edited files: `TransferEventConsumer`, `NotificationController`, `SseHub`, `InMemoryProcessedEventStore`, and the test-import updates); line-by-line explanation; under-the-hood; checkpoint; commit; pitfall.

### F2: No run-and-see anywhere in the build — nothing verifies intermediate states

**Severity:** high
**Lens:** swe
**Location:** Sub-steps 1-4 (lines 198-218)
**needsRun:** true
**Issue:** A multi-file package move breaks compilation at every intermediate point, yet no sub-step has a run-and-see block. Sub-step 4 says "Run them: green" without a command-plus-expected-output pair or a common-wrong-output. The learner has no way to know whether their state after each sub-step is correct.
**Fix:** After each sub-step, add a run-and-see: exact command (`./mvnw -pl services/notification test-compile` after sub-steps 1-3; `./mvnw -pl services/notification test` after sub-step 4), the real pasted output from actually running it at that state, and a common-wrong-output (e.g. `cannot find symbol: class TransferEvent` from a stale import, `package com.buildabank.notification.domain does not exist` from a missed package line).

### F3: 12-hour step with no session plan, time-boxes, re-entry lines, or progress markers

**Severity:** high
**Lens:** adhd
**Location:** Whole lesson (Orient states "≈ 12 hours focused", line 37)
**needsRun:** false
**Issue:** There is no sitting plan, no time-box on any movement or sub-step, no named save points, no "stopping here? next session starts at..." re-entry lines, and no you-are-here / "sub-step X of 4" markers. An ADHD learner facing a 12-hour restructure has no scaffolding for splitting, pausing, or resuming the work.
**Fix:** Add a session-plan table after "Before You Start" (e.g. 5 sittings of ~2.5h: S1 Orient+Understand; S2 Sub-step 1 domain; S3 Sub-step 2 application+ports; S4 Sub-step 3 adapters; S5 Sub-step 4 proof + Prove/Apply/Review), each with a named save point ("domain compiles, committed"). Add a time-box to each movement heading and each sub-step. At each sub-step's checkpoint add a re-entry line ("Stopping here? You have the pure domain compiling; next session starts at Sub-step 2, first action: create `application/port/in/NotifyOnTransfer.java`"). Add "Sub-step X of 4" markers.

### F4: No first win — nothing runs until the very end of the build

**Severity:** high
**Lens:** adhd
**Location:** "Your Starting Point" / Sub-step 1 (lines 194-200)
**needsRun:** true
**Issue:** The first runnable moment is Sub-step 4's test run, hours in. Reward-loop density for the whole build is effectively one run. Nothing visibly runs within the first 10 minutes.
**Fix:** Open the build with a 5-minute baseline win: run `./mvnw -pl services/notification test` at `step-26-start`, paste the real 7-tests-green output, and frame the entire step as "keep these 7 green while we move every class." This doubles as the before-side of the behaviour-preservation proof. (Requires a real run to paste truthful baseline output.)

### F5: Verification Log output is elided and annotated while claiming "Real output below"

**Severity:** medium
**Lens:** swe
**Location:** "D · Prove It Works", block 1 (lines 244-252)
**needsRun:** true
**Issue:** The surefire lines are truncated with `…` and carry inline editorial parentheses ("(UNCHANGED assertions — DLT still works)") inside the code fence. This is edited output presented under a "Real output below" claim — it undermines the log's evidentiary value and teaches the learner that doctored logs are acceptable proof.
**Fix:** Re-run `./mvnw -pl services/notification test` and paste the raw surefire summary lines verbatim (full `Tests run: N, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ...` per class); move the "unchanged assertions" commentary to prose outside the code fence.

### F6: No guidance on how to actually perform the package move

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-step 1 (lines 198-200), Troubleshooting (line 306)
**needsRun:** false
**Issue:** The mechanical skill being taught — moving Java classes across packages (create directories, `git mv`, edit the `package` declaration, fix imports in main and test trees) — is never shown. Troubleshooting mentions fixing test imports but the build never explains the procedure, and a beginner without IDE refactoring support will flounder.
**Fix:** In Sub-step 1, add the explicit procedure: an old-path → new-path mapping table for all 9 classes (the B→C bridge tree already lists them), the `mkdir` + `git mv` commands (or the IDE Move-Class alternative, named), the `package` line edit each file needs, and "run `./mvnw -pl services/notification test-compile`; every `cannot find symbol` is a stale import — fix and re-run."

### F7: Build closes with no sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** "The Finished Result" (lines 229-231)
**needsRun:** false
**Issue:** The contract requires the build to close with a sequence diagram of the flow built; the build ends in two sentences of prose. The hexagon's runtime flow through ports is exactly what a sequence diagram would cement.
**Fix:** Add a Mermaid `sequenceDiagram` before "The Finished Result": Kafka record → `TransferEventConsumer` (adapter/in/messaging) → `TransferEventParser` → `NotifyOnTransfer.handle` → `NotificationService` → `ProcessedEventStore.markIfNew` (adapter/out/persistence) → `NotificationPublisher.publish` → `SseHub` (adapter/out/push) → browser, with the duplicate branch (markIfNew=false → no publish).

### F8: Flashcards not in the lesson — only a pointer

**Severity:** medium
**Lens:** structure
**Location:** Recap item (g) (line 335)
**needsRun:** false
**Issue:** The recap says flashcards are "appended to docs/flashcards.md" but the contract requires 3-5 flashcards inline in the lesson; the learner cannot review them from this file.
**Fix:** Inline 4 flashcards in the recap: (1) Q: State the dependency rule / A: source dependencies point inward, domain depends on nothing; (2) Q: inbound vs outbound port / A: offered (called by driving adapters) vs needed (implemented by driven adapters); (3) Q: why keep the domain framework-free / A: survives infra churn, microsecond-testable, correct change direction; (4) Q: when do you skip aggregates/repositories / A: thin context with no invariant-guarding consistency boundary.

### F9: Interactivity toolkit nearly absent from the build

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-steps 1-4 + "Play With It" (lines 198-227)
**needsRun:** false
**Issue:** One predict-then-run (Sub-step 2) is the build's entire interactivity. No type-it-yourself, no learner-facing break-it-on-purpose, no knowledge-checks. There is also no scaffold fading — nothing is worked first and then handed over.
**Fix:** Add: (a) a break-it-on-purpose after Sub-step 1 — add `import org.springframework.stereotype.Component;` to `domain/Notification.java`, observe the build still passes, and name that as the erosion problem Step 27's ArchUnit will fail the build on; then revert. (b) A type-it-yourself in Sub-step 2: show `NotifyOnTransfer` fully worked, then have the learner write `NotificationPublisher` themselves from its one-line spec (fading). (c) A knowledge-check after Sub-step 3: "which ring may import Jackson, and why?"

### F10: Big Idea lacks the contract's analogy

**Severity:** low
**Lens:** pedagogy
**Location:** "The Big Idea" (lines 105-129)
**needsRun:** false
**Issue:** The contract calls for big idea + diagram + analogy; the diagram is there, the analogy is not ("close cousin of Clean/Onion" is a comparison, not an analogy).
**Fix:** Add the standard socket analogy after the ring list: a wall socket (port) is a shape the house (core) defines; any compliant plug (adapter) fits — lamp, vacuum, laptop charger — and you swap appliances without rewiring the house. Kafka, SSE, and the dedup store are appliances.

### F11: Optional content has no time-cost labels

**Severity:** low
**Lens:** adhd
**Location:** "Go Deeper" (lines 278-282) and "Your Turn" stretch (line 296)
**needsRun:** false
**Issue:** The two Go Deeper collapsibles and the stretch exercise carry no time estimates, so a time-blind learner cannot budget them against the 12-hour step.
**Fix:** Label each: "Go Deeper (optional, ~10 min read)" on both collapsibles; "Stretch (~45-60 min)" on the second-adapter challenge; "Quick (~15 min)" on the two quick exercises.

### F12: requests.http missing from the step folder

**Severity:** low
**Lens:** structure
**Location:** `steps/step-26/` (folder contains only lesson.md and smoke.sh)
**needsRun:** false
**Issue:** The step folder has no `requests.http`. Even in a behaviour-preserving step, a manual probe of the surviving HTTP surface (the SSE subscribe endpoint through the gateway) would let the learner see behaviour preserved, not just test it.
**Fix:** Add `steps/step-26/requests.http` with the SSE subscribe request (as fronted since Step 20/30) and a comment that the response must be identical to Step 25's — or, if the fleet convention permits omission for non-HTTP steps, add a one-line note in the Cheat Card stating why there is no requests.http this step.
