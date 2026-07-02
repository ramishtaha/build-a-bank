# Step 20 audit - swe:6 pedagogy:4 adhd:3 structure:5 - thinBuild:true

## Strengths

- The Verification Log (Movement D) is exemplary: tier stated (Full), realistic pasted Maven/test output, a mutation check with real failure evidence, hard-to-fake broker artifacts, and an explicit honesty note distinguishing what was proven in tests vs. the manual three-process demo.
- Conceptual prose is technically accurate and interview-sharp: dual-write problem, Outbox trade-offs (AFTER_COMMIT-publish gap, polling vs CDC), at-least-once + idempotency = exactly-once effect, and the Boot-4 `spring-boot-starter-kafka` gotcha are all correct and well-motivated.
- Strong cross-step connective tissue: explicit "Depends on: Steps 19, 12, 14, 13" line, one-line callbacks to Steps 11/12/14/19, and forward links to Steps 21 and 54.

## Missing spine

- Complete code for every build sub-step - Movement C contains zero code blocks (no Java, no `V3__outbox.sql`, no `pom.xml`, no `application.yml`)
- Per-sub-step micro-anatomy: exact file location, line-by-line explanation, run-and-see (command + expected output + common-wrong-output), checkpoint, per-sub-step commit (only one commit for the whole build)
- Closing sequence diagram of the flow built
- What-we-will-build Mermaid diagram at the build opening (the B->C bridge has only the file tree; the flowchart lives up in Understand)
- Analogy in the Big Idea
- 3-5 flashcards inline in the recap (deferred to `docs/flashcards.md`, not shown)
- Session/sitting plan with named save points for a ~20-hour step
- Time-boxes per movement and per sub-step (only the whole-step "~20 hours" estimate)
- You-are-here markers / "sub-step X of N" progress indicators
- Type-it-yourself, break-it-on-purpose, and knowledge-checks in the build
- Re-entry support lines at checkpoints

## Findings

### F1: Build movement contains no code at all

**Severity:** high
**Lens:** swe
**Location:** C - "Let's Build It", Sub-steps 1-4 (lines ~215-264)
**needsRun:** false
**Issue:** The sacred build is a 4-paragraph outline. Each sub-step is a 2-5 line Goal summary; not one file is shown - no `TransferCompletedEvent`, `OutboxEvent`, `OutboxWriter`, `OutboxRelay`, `V3__outbox.sql`, `SseHub`, `TransferEventConsumer`, `NotificationController`, no pom or yml edits. A nervous beginner at 11pm cannot write ~10 new files plus a new Maven module from a file-tree listing. Every claim in Prove (D) references code the learner never sees.
**Fix:** For each sub-step, paste the complete implementation from the repo at tag `step-20-end` (all files in the B->C bridge tree), each with a file-path header comment, package line, and full imports; show `TransferService.post()`, both `pom.xml` changes, and root `pom.xml` module registration as diffs; follow each file with a line-by-line explanation and an under-the-hood note. Source the code from the working tree - do not write it from memory.

### F2: No run-and-see, no checkpoints - nothing runs until the very end

**Severity:** high
**Lens:** structure
**Location:** C - Sub-steps 1-4; first runnable moment is "Play With It" (line ~246)
**needsRun:** true
**Issue:** The micro-anatomy contract requires run-and-see (exact command + expected output + common-wrong-output) and a checkpoint per sub-step. None exist. The learner types (or would type) an entire event pipeline plus a new service with zero feedback until a multi-service manual demo at the end - also destroying the reward loop and first-win-fast (nothing visibly runs in the first 10 minutes of the build).
**Fix:** After each sub-step add a real captured run: sub-step 1/2 -> `./mvnw -pl services/demand-account test -Dtest=OutboxWriteTest` with pasted output; sub-step 3 -> `OutboxRelayKafkaTest` run; sub-step 4 -> notification tests plus a `curl -N http://localhost:8084/api/notifications/stream` snippet showing a live event line. Add one common-wrong-output each (e.g., the missing-`spring-boot-starter-kafka` `KafkaTemplate` error for sub-step 3). Outputs must come from actually running these commands, and each sub-step ends with a checkpoint checklist and its own commit line.

### F3: No session plan, time-boxes, or re-entry support for a ~20-hour step

**Severity:** high
**Lens:** adhd
**Location:** A - "This Step in 30 Seconds" (Effort row, line ~37) and throughout C
**needsRun:** false
**Issue:** The step self-declares ~20 hours but offers no sitting plan, no per-movement or per-sub-step time-boxes, and no re-entry lines. An ADHD learner has no way to slice this into sessions or to resume after stopping mid-build.
**Fix:** Add a "Session plan" table in Orient: 7-8 sittings of 2-3h with named save points (e.g., S1 Understand + sub-step 1; S2 sub-step 2 + OutboxWriteTest green; S3 sub-step 3 + relay test green; S4-5 notification service; S6 Play With It + Prove; S7 Apply/Review). Put a time estimate next to each movement heading and each sub-step title, and end every sub-step checkpoint with a re-entry line: "Stopping here? You have X working. Next session starts at sub-step N; first action: <command>."

### F4: Build lacks its opening diagram and closing sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** "B->C bridge: what we'll build" (line ~188) and "The Finished Result" (line ~262)
**needsRun:** false
**Issue:** The contract requires the build to open with a what-we-will-build Mermaid diagram plus files tree, and to close with a sequence diagram of the flow built. The bridge has only the file tree (the flowchart sits earlier in Understand), and there is no closing sequence diagram at all.
**Fix:** Duplicate (or move) the pipeline flowchart into the B->C bridge above the file tree. After "The Finished Result", add a Mermaid `sequenceDiagram`: client -> demand-account `POST /transfers` -> tx writes ledger + outbox_event -> commit -> OutboxRelay poll -> Kafka `transfers.completed` -> notification `@KafkaListener` (dedupe by eventId) -> SseHub -> browser via `text/event-stream`.

### F5: TransferEventListener's actual role is unexplained magic

**Severity:** medium
**Lens:** swe
**Location:** C - Sub-step 1 (line ~222) vs Sub-step 2 (line ~228)
**needsRun:** false
**Issue:** Sub-step 1 builds an `@TransactionalEventListener(AFTER_COMMIT)` listener; sub-step 2 then says `TransferService.post()` calls `outbox.write(event)` directly in the same transaction. The lesson never says what the AFTER_COMMIT listener actually does, and the Understand section even warns that publishing from AFTER_COMMIT reintroduces the lost-event gap - so the listener looks redundant or contradictory. `OutboxWriteTest` asserts it "fires once" but its effect is never stated.
**Fix:** Add a short paragraph in sub-step 1 stating exactly what the listener does in this codebase (matching the real code - e.g., logging/metrics only) and why the outbox write cannot live in an AFTER_COMMIT listener (it must be inside the transaction to be atomic). If the repo code differs, restate the sub-step to match the repo.

### F6: Interactivity toolkit nearly absent; no scaffold fading

**Severity:** medium
**Lens:** pedagogy
**Location:** C - Sub-steps 1-4
**needsRun:** false
**Issue:** The whole build has exactly one predict-then-run (sub-step 1). No knowledge-checks, no type-it-yourself, no fading from worked example to independent work - sub-step 4 (a second listener + controller, patterns the learner has now seen) is presented identically to sub-step 1.
**Fix:** Add a predict-then-run per sub-step (e.g., sub-step 3: "if the broker is down when the relay runs, is the outbox row marked published?"), 2-3 inline knowledge-checks (e.g., after sub-step 2: "which crash windows can still lose the event now? none - why?"), and convert sub-step 4's `NotificationController` + `SseHub` into a guided type-it-yourself with a spec checklist and the reference solution in a collapsed `<details>`.

### F7: No break-it-on-purpose exercise

**Severity:** medium
**Lens:** pedagogy
**Location:** C - Sub-steps 1-3 / "Play With It"
**needsRun:** true
**Issue:** This step has two perfect, cheap breakages that would cement its core misconceptions, and neither is offered as a learner exercise (the mutation appears only in the author's Verification Log, not as something the learner does).
**Fix:** Add one break-it-on-purpose box: swap `@TransactionalEventListener(AFTER_COMMIT)` for plain `@EventListener`, run the overdraft-rollback test, and paste the real failing output showing the listener fired for a transfer that never happened; or temporarily replace `spring-boot-starter-kafka` with bare `spring-kafka` and paste the real `KafkaTemplate` bean error. Both require actually running the broken build to capture truthful output; end with the revert command.

### F8: Flashcards not in the lesson

**Severity:** medium
**Lens:** structure
**Location:** F - Recap item (g) (line ~375)
**needsRun:** false
**Issue:** The recap says flashcards are "appended to `docs/flashcards.md`" but the contract requires 3-5 flashcards inline in the recap; a learner reviewing the lesson has nothing to drill.
**Fix:** Inline 4 Q/A flashcards in item (g): (1) dual-write problem -> two systems, no shared tx, crash loses/invents events; (2) what makes the outbox write atomic -> same DB transaction as the ledger change; (3) exactly-once effect -> at-least-once delivery + consumer dedupe by stable eventId; (4) SSE vs WebSocket -> one-way HTTP stream w/ auto-reconnect vs bidirectional. Keep the `docs/flashcards.md` pointer as an extra.

### F9: No analogy in the Big Idea

**Severity:** low
**Lens:** pedagogy
**Location:** B - "The Big Idea - decoupling with events" (line ~112)
**needsRun:** false
**Issue:** The contract's Understand movement requires big idea + diagram + analogy; the diagram is there but no analogy anchors the Outbox for novices.
**Fix:** Add two sentences after the diagram: the Outbox is the office outgoing-mail tray - finishing the paperwork and dropping the letter in the tray is one act (same transaction); the mail clerk (relay) collects the tray on rounds and gets a receipt from the post office (Kafka ack) before marking anything sent.

### F10: Event payload schema never shown; money apparently serialized as floating-point

**Severity:** low
**Lens:** swe
**Location:** C - Sub-step 1; D - smoke.sh log ("Transfer of 40.0", line ~305)
**needsRun:** false
**Issue:** The wire contract of `TransferCompletedEvent` (the thing two services must agree on, across Jackson 2 and Jackson 3) is never shown, and the smoke log's "40.0" suggests the amount travels as a floating-point number - a money-handling smell the course otherwise teaches to avoid (BigDecimal).
**Fix:** In sub-step 1, paste the actual event JSON as it appears on the topic (from the repo/test output, not invented) and add a one-line note on the amount type; if the repo really serializes amount as a JSON float, add a caution box that a production event contract should carry decimal-as-string or minor units, previewing the Step 21 payment contract.

### F11: No progress markers and DoD is prose, not a checklist

**Severity:** low
**Lens:** adhd
**Location:** C - sub-step headings and "The Finished Result" (line ~262)
**needsRun:** false
**Issue:** Sub-steps are unnumbered against a total (no "sub-step X of 4", no you-are-here markers), and the Definition of Done is a single run-on sentence - both hurt visible progress and completion confidence.
**Fix:** Retitle headings "Sub-step 1 of 4 - ..." etc., add a one-line you-are-here strip (outbox -> relay -> consumer -> SSE, current stage bolded) at the top of each sub-step, and convert the DoD into a 4-item checkbox list (live notification seen; `./mvnw verify` green; `smoke.sh` passes; `step-20-end` committed/tagged).
