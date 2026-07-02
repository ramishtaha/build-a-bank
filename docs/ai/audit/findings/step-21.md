# Step 21 audit - swe:5 pedagogy:4 adhd:3 structure:5 - thinBuild:true

## Strengths

- Verification Log (D) is exemplary: tier stated (Full), realistic pasted Maven/test output, a genuine mutation check with the failing assertion output shown and reverted, clean-room run, smoke.sh, and an honesty note about what was and wasn't demoed live.
- Understand (B) is technically accurate and interview-aligned: orchestration-vs-choreography trade-off, the "Saga is not isolated" misconception called out explicitly, 2PC then-vs-now, and correct rationale for Redis `SET NX EX` and `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`.
- Strong prior-step callbacks (Steps 12/14/19/20 each tied to what changes here) and a troubleshooting section grounded in a real experienced failure (cached Spring context sharing consumer state across `@SpringBootTest` classes).

## Missing spine

- Analogy in the B Understand big-idea section (diagram present, analogy absent)
- Complete code listings with file-path header comments for every build sub-step (zero code in the entire build)
- Line-by-line explanations (absent for all 5 sub-steps)
- Per-sub-step under-the-hood (absent for all 5 sub-steps)
- Run-and-see (exact command + expected output + common-wrong-output) — absent for all 5 sub-steps
- Checkpoints — absent for all 5 sub-steps
- Pitfalls for sub-steps 1-4 (only sub-step 5 has one)
- What-we-will-build Mermaid diagram at the top of C Build (files-we-will-touch tree exists, flow diagram does not)
- Closing sequence diagram of the flow built (end of C Build)
- 3-5 flashcards inline in the recap (only a pointer to `docs/flashcards.md`)
- Definition-of-Done as an actual checklist (currently one prose sentence)
- Type-it-yourself and break-it-on-purpose interactions inside the build

## Findings

### F1: Build is a skeleton — five stub sub-steps with no code, no run-and-see, no checkpoints

**Severity:** high
**Lens:** swe
**Location:** "C · 🛠️ Let's Build It — Step by Step", Sub-steps 1-5 (lines 189-241)
**needsRun:** true
**Issue:** The sacred build for an ≈18-hour money-path step is ~50 lines. Every sub-step is a 2-6 line goal statement: no file locations beyond class names, not one line of Java/YAML/pom code, no imports, no line-by-line explanation, no run-and-see with expected output, no checkpoints. The Verification Log then cites `PaymentSagaTest`, `PaymentControllerTest`, and `DeadLetterTest` — tests the learner is never shown how to write. A nervous beginner at 11pm cannot produce `PaymentStepService`, `PaymentService`, `RedisIdempotencyStore`, the controller/DTOs/exception mapping, the Redis config/dependency, or `KafkaErrorHandlingConfig` from these one-liners.
**Fix:** Expand each of the 5 sub-steps to the full micro-anatomy: Goal → exact file path → complete listing with file-path header comment, package line, and all imports (diff view for the `GlobalExceptionHandler`, `TransferEventConsumer`, `application.yml`, and `pom.xml` edits) → line-by-line → under-the-hood → predict-then-run → run-and-see with the exact `./mvnw -pl services/... test -Dtest=...` command, real pasted output, and a common-wrong-output → checkpoint → commit → pitfall. Include writing the three tests named in the Verification Log as part of the build (e.g., tests written alongside each sub-step). Run every command to capture truthful output.

### F2: 18-hour step has no session plan and no time-boxes anywhere

**Severity:** high
**Lens:** adhd
**Location:** "A · 🧭 Orient" — 30-seconds table (line 36) and the whole lesson
**needsRun:** false
**Issue:** Effort is stated as ≈18 hours focused, but there is no sitting plan (contract: 6-10 planned sittings of ~2-3h with named save points), and no time-box appears on any movement or sub-step — the only time figure in the entire lesson is the total.
**Fix:** After the 30-seconds table, add a "Session plan" table with ~7 sittings, e.g.: S1 Orient+Understand (2h, save point: none needed), S2 Sub-step 1 Saga steps + test (2.5h, save: `PaymentStepService` committing independently), S3 Sub-step 2 orchestrator + compensation test (2.5h, save: first commit), S4 Sub-step 3 Redis idempotency (2.5h), S5 Sub-step 4 endpoint + 422 mapping (2h, save: demand-account commit), S6 Sub-step 5 DLT + test (2.5h, save: notification commit), S7 Prove+Apply+Review (2-3h, save: `step-21-end` tag). Add a "≈ Xh" tag to each movement heading and each sub-step heading.

### F3: No formative checks inside the build — one predict, zero knowledge-checks, zero type-it-yourself

**Severity:** high
**Lens:** pedagogy
**Location:** "C · 🛠️ Let's Build It" Sub-steps 1-5 (lines 195-221)
**needsRun:** false
**Issue:** The only interactive element in the entire build is one predict-then-run in Sub-step 1. There are no knowledge-checks, no type-it-yourself prompts, no break-it-on-purpose, and (see F1) no checkpoints — so none of the five stated objectives is verified by a formative check before the Prove movement, and there is no scaffold fading because nothing is worked in the first place.
**Fix:** Add a predict-then-run to each of sub-steps 2-5 (e.g., Sub-step 2: "what HTTP status should a failed-then-compensated payment return, and why not 500?"; Sub-step 3: "what does `setIfAbsent` return on the second call?"; Sub-step 5: "with `FixedBackOff(0, 2)`, how many total delivery attempts before the DLT?"). Add one knowledge-check after Sub-step 2 (orchestration vs choreography, matching objective 2) and one after Sub-step 3 (why Redis vs the Step-14 DB store). Convert Sub-step 5's DLT config into a type-it-yourself with a collapsed reference solution (scaffold fading), and turn the "produce a malformed message" experiment into a labeled break-it-on-purpose inside Sub-step 5.

### F4: The only API guidance given is non-compiling shorthand

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 1 (line 197) and Sub-step 5 (line 217)
**needsRun:** false
**Issue:** Because no real code exists (F1), the prose shorthand is all the learner has to type from, and it won't compile: `@Transactional(REQUIRES_NEW)` is not valid Java (must be `@Transactional(propagation = Propagation.REQUIRES_NEW)` with the `Propagation` import), and `new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template, → topic+".DLT"), new FixedBackOff(0, 2))` uses a pseudo-arrow where a real `BiFunction<ConsumerRecord<?,?>, Exception, TopicPartition>` lambda is required.
**Fix:** In Sub-step 1, write the annotation in full with its import and one sentence naming `Propagation.REQUIRES_NEW`. In Sub-step 5, replace the arrow pseudo-code with the actual lambda, e.g. `(record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())`, and explain the `BiFunction` destination-resolver parameter on first appearance. (Full listings are F1; this fix ensures even the prose is copy-safe.)

### F5: Idempotency store's record-after-success design has unacknowledged failure windows in Understand

**Severity:** medium
**Lens:** swe
**Location:** "🌱 Under the Hood: idempotency in Redis" (lines 136-142) and Sub-step 3 (lines 205-207)
**needsRun:** false
**Issue:** The taught flow is check key → run Saga → record key on success. That leaves two real gaps in a money path presented as "you must not charge twice": (a) two concurrent requests with the same key both pass the check before either records (the atomic `SET NX` is only used at record time, after both have paid); (b) a crash after the Saga commits but before the Redis record means the client's retry pays again. The reserve-then-complete fix exists only as an unexplained "Quick" exercise in Apply, so the Understand section overstates the guarantee.
**Fix:** Add a caveat paragraph to "Under the Hood: idempotency in Redis": name both windows explicitly, state that this step's implementation is safe for the common sequential-retry case only, and forward-reference the reserve-then-complete (`SET key PENDING NX EX` first, overwrite with the result after) Your-Turn exercise as the production hardening. Also add one sentence on choosing the TTL (must exceed the client's maximum retry horizon).

### F6: Build has no opening what-we-will-build diagram and no closing sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** "C · 🛠️ Let's Build It" open (line 189) and "🏁 The Finished Result" (line 239)
**needsRun:** false
**Issue:** The contract requires the build to open with a what-we-will-build Mermaid diagram (the files tree exists in the B→C bridge, the flow diagram does not) and to close with a sequence diagram of the flow built. Neither is present; the only Mermaid in the lesson is the 4-node flowchart in Understand.
**Fix:** At the top of C (after "Your Starting Point"), add a Mermaid flowchart showing client → `PaymentController` → `PaymentService` (orchestrator) → `PaymentStepService` (debit/credit/refund, each own tx) + Redis (idempotency) + the notification consumer → `transfers.completed.DLT`. Before "The Finished Result", add a Mermaid `sequenceDiagram` of the two proven paths: happy retry (Redis hit → original paymentId) and compensation (debit commits, credit fails, refund runs, 422 returned).

### F7: Flashcards are not in the lesson

**Severity:** medium
**Lens:** structure
**Location:** "🏆 Recap & Study Notes" item (g) (line 345)
**needsRun:** false
**Issue:** The recap says flashcards are "appended to `docs/flashcards.md`" but the contract requires 3-5 flashcards in the lesson's recap; none are shown, so a learner reviewing the lesson has nothing to drill.
**Fix:** Replace item (g) with 4 inline Q/A flashcards (front/back), e.g.: "Why can't a committed Saga step be rolled back? → It committed in its own local tx; you run a compensating action (refund) instead" · "Orchestration vs choreography? → central coordinator drives steps vs services react to events" · "How does `SET key val NX EX ttl` give idempotency? → atomic claim-if-absent with expiry; retry reads the stored result" · "What does a Dead-Letter Topic prevent? → a poison message blocking the partition forever". Keep the pointer to `docs/flashcards.md` as a secondary note.

### F8: No progress markers or re-entry support anywhere in the build

**Severity:** medium
**Lens:** adhd
**Location:** Sub-step headings (lines 195, 201, 205, 209, 215) and the two commit points (lines 213, 221)
**needsRun:** false
**Issue:** No "Sub-step X of 5" / you-are-here markers, and neither commit point tells a learner who stops there what they have working and where the next session starts — for a multi-sitting 18-hour step, resuming requires re-reading the whole build.
**Fix:** Rename headings to "Sub-step 1 of 5 — ..." through "Sub-step 5 of 5 — ...". After each commit, add a re-entry block: e.g. after the Sub-step 4 commit — "Stopping here? You have a working, idempotent payment Saga behind `POST /api/v1/payments`. Next session starts at Sub-step 5 of 5; first action: open `services/notification/src/main/java/.../KafkaErrorHandlingConfig.java`." Do the same after Sub-step 5's commit pointing at Movement D.

### F9: No first win — nothing runs until after all five sub-steps

**Severity:** medium
**Lens:** adhd
**Location:** "C · 🛠️ Let's Build It" — first runnable feedback is "🎮 Play With It" (line 223)
**needsRun:** true
**Issue:** The reward loop is empty: the learner writes an orchestrated Saga, a Redis store, a controller, and Kafka error handling across ~15+ hours before anything visibly runs. The contract wants something running within ~10 minutes of starting the build and a run/see per sub-step.
**Fix:** When expanding the build (F1), open Sub-step 1 with an immediate first win: start Redis (`docker run -d --name bank-redis -p 6379:6379 redis:7.4-alpine`) and run a 5-line `redis-cli SET pay:demo 123 NX EX 60` / repeat-and-see-`nil` demonstration (or a one-command failing-then-passing first test), pasting the real output; then ensure every sub-step ends in an executed run-and-see so the learner sees a result at least once per sitting.

### F10: Big-idea analogy missing and Definition of Done is prose, not a checklist

**Severity:** low
**Lens:** structure
**Location:** "🧠 The Big Idea" (lines 105-118) and "🏁 The Finished Result" (line 241)
**needsRun:** false
**Issue:** The contract's Understand movement requires big idea + diagram + analogy — the analogy is absent. The Definition of Done is a single run-on sentence rather than a checkable list.
**Fix:** Add a 2-3 sentence analogy to the Big Idea (e.g., booking a trip: flight, hotel, and car are booked separately; if the hotel falls through you don't "un-happen" the flight purchase — you cancel it for a refund. Cancellation is the compensating transaction). Convert the DoD sentence into 5 checkboxes: retry-with-same-key returns same paymentId & moves money once · failed credit leaves source balance restored · poison message lands on `transfers.completed.DLT` · `./mvnw verify` green + `smoke.sh` passes · committed and tagged `step-21-end`.
