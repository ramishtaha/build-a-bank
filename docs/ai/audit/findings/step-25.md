# Step 25 audit - swe:6 pedagogy:4 adhd:3 structure:4 - thinBuild:true

## Strengths

- Technically accurate and repo-verified: every claim (thin consumer, `ProcessedEventStore` port + `InMemoryProcessedEventStore` adapter, `TransferEventParser`, unchanged `TransferEventConsumerKafkaTest`/`DeadLetterTest`/`NotificationControllerTest`, tags `step-25-start`/`step-25-end`) matches the actual code at tag `step-25-end`.
- Strong Prove section for its tier: stated Standard tier, a genuine §12.3 mutation check (break `markIfNew`, watch the unit test fail, revert), and a §12.8 honesty note scoping the refactor.
- Excellent conceptual framing: the cheat-card before/after ASCII, the smell → refactoring table, and go-deeper/interview items that pre-empt real misconceptions (YAGNI, anemic over-splitting, "don't edit tests and code together").

## Missing spine

- Build sub-step micro-anatomy absent for ALL 4 sub-steps: no code (not one snippet in the entire Build), no exact file paths, no line-by-line, no under-the-hood, no predict-then-run, no run-and-see, no checkpoints, no per-sub-step commits, no pitfalls (single commit line only, after sub-step 4).
- No what-we-will-build Mermaid diagram at the Build open (files-we-will-touch tree exists in the B→C bridge).
- No closing sequence diagram of the refactored flow.
- No diagram and no analogy in Understand's big idea (contract: big idea + diagram + analogy).
- Interactivity toolkit essentially absent: no predict-then-run, no type-it-yourself, no knowledge-checks (the one "break-it-on-purpose" is just "run the tests").
- No session plan / sittings, no time-boxes per movement or sub-step, no re-entry lines, no you-are-here markers (≈10-hour step).
- No explicit `git checkout step-25-start` command in "Your Starting Point".
- `requests.http` absent from `steps/step-25/` (`smoke.sh` present; neighbors step-24/step-26 also lack it — likely convention for non-HTTP steps).
- Flashcards not inline — deferred to `docs/flashcards.md` (verified: 5 step-25 cards exist there; appears to be course convention).

## Findings

### F1: Build is a skeleton — entire micro-anatomy and all code missing

**Severity:** high
**Lens:** structure
**Location:** "C · 🛠️ Let's Build It", Sub-steps 1–4 (lines 167–203)
**needsRun:** true
**Issue:** Each sub-step is a single 🎯 goal sentence naming classes the learner has never seen. There is not one line of Java in the whole Build: no `TransferEvent` record definition, no `TransferEventParser` API, no port interface, no diff of `TransferEventConsumer`/`Notification`, no unit-test code. A nervous beginner at 11pm cannot produce six files and two tests from class names alone — this violates the sacred-build contract for every sub-step.
**Fix:** Rewrite the Build with full micro-anatomy per sub-step. All finished code already exists at tag `step-25-end` (`services/notification/src/main/java/com/buildabank/notification/{TransferEvent,TransferEventParser,ProcessedEventStore,InMemoryProcessedEventStore}.java`, plus diffs for `TransferEventConsumer.java` and `Notification.java`, and tests `TransferEventParserTest.java`, `InMemoryProcessedEventStoreTest.java`). Present new files complete (file-path header, package, imports), edits as before/after diffs, each followed by line-by-line explanation, under-the-hood, predict-then-run, a run-and-see (`./mvnw -pl services/notification test`, or `-Dtest=TransferEventParserTest` for the fast loop) with real pasted output and one common-wrong-output, a checkpoint, a per-sub-step commit, and a pitfall. Run the commands at each intermediate state to capture truthful output.

### F2: No session plan, time-boxes, re-entry lines, or progress markers for a 10-hour step

**Severity:** high
**Lens:** adhd
**Location:** "This Step in 30 Seconds" table (Effort row, line 37) and throughout Build
**needsRun:** false
**Issue:** Effort says "≈ 10 hours focused" but there is no sitting plan, no per-movement or per-sub-step time-boxes, no named save points, no "stopping here? next session starts at..." re-entry support, and no you-are-here / "sub-step X of 4" markers.
**Fix:** Add a "Session plan" box in Orient splitting the step into 4 sittings of ~2.5h (S1: Orient+Understand; S2: sub-steps 1–2; S3: sub-steps 3–4; S4: Prove+Apply+Review), each with a named save point (e.g. "end of S2: parser + port compile, module tests green, committed"). Add a time-box and "Sub-step N of 4" marker to each sub-step heading, and a re-entry line at each checkpoint ("Stopping here? You have X working; next session starts at sub-step N, first action: ...").

### F3: No first win and no reward loop — nothing runs until after all four sub-steps

**Severity:** high
**Lens:** adhd
**Location:** "Your Starting Point" (line 169) through "Play With It" (line 194)
**needsRun:** true
**Issue:** The first command the learner executes is in Play With It, after hours of refactoring. Worse, this contradicts the step's own core discipline ("small structural moves under a green suite"): the build never tells the learner to run the safety-net tests before starting or between moves.
**Fix:** Add a baseline run as the first action of the build (within ~10 minutes): `git checkout step-25-start` (or confirm `git describe` → `step-24-end`) then `./mvnw -pl services/notification test` with real pasted output, framed as "this green suite is your safety net — memorize what green looks like". Then add a run-and-see at the end of every sub-step re-running the module suite (real output captured at each intermediate state), so the learner sees the suite stay green after each structural move.

### F4: No diagrams anywhere — Understand, build-open, and closing sequence diagram all missing

**Severity:** medium
**Lens:** structure
**Location:** "B · 🧠 Understand" (line 103), "B→C bridge" (line 150), end of Build (line 203)
**needsRun:** false
**Issue:** The contract requires a diagram + analogy in Understand, a what-we-will-build Mermaid at the build open, and a closing sequence diagram. None exist; the lesson is diagram-free.
**Fix:** Add (1) in Understand, a Mermaid before/after component diagram (god-method consumer with inline map/ObjectMapper vs thin consumer → parser / port+adapter / factory / SseHub) plus a one-paragraph analogy (port = wall socket, adapter = plug: the lamp doesn't care which power plant); (2) at the build open, a small what-we-will-build Mermaid next to the existing files tree; (3) after "The Finished Result", a `sequenceDiagram`: Kafka → TransferEventConsumer → TransferEventParser → ProcessedEventStore → Notification.from → SseHub, with the duplicate-skip and poison→DLT branches.

### F5: No knowledge-checks or predict-then-run; the one interactive beat is trivial

**Severity:** medium
**Lens:** pedagogy
**Location:** Understand section and Build sub-steps 1–4 (lines 103–199)
**needsRun:** false
**Issue:** The interactivity toolkit is absent: zero predicts, zero knowledge-checks, no type-it-yourself, and the sole "break-it-on-purpose" (sub-step 3) is just "run the unchanged tests". No scaffold fading is possible because nothing is worked to begin with.
**Fix:** Add 2–3 knowledge-checks in Understand (e.g. "classify: a method that parses JSON, dedupes, and logs — which smells?"; "which SOLID letter does the port serve?" with `<details>` answers). Add a predict before each sub-step's run ("If the parser caught its own exception instead of throwing, which unchanged test would fail? Answer: DeadLetterTest"). Make sub-step 4 partially type-it-yourself (give the parser test worked, have the learner write the store test from a signature-level spec). These can reference the mutation outcome already evidenced in Prove §2 without new runs.

### F6: Stretch exercise promises a reference solution that does not exist

**Severity:** medium
**Lens:** swe
**Location:** "Your Turn: Practice & Challenges", Stretch bullet (line 266)
**needsRun:** false
**Issue:** "reference solution in `solutions/step-25/`" — the `solutions/` directory contains only `step-01`. A learner who attempts the Redis adapter and gets stuck hits a dead reference.
**Fix:** Delete the parenthetical "(reference solution in `solutions/step-25/`)" (or replace with "no reference solution — compare against Step 21's Redis wiring"), unless a reference solution is actually added to `solutions/step-25/` as separate work.

### F7: Verification Log output blocks are visibly edited while claiming "Real output below"

**Severity:** low
**Lens:** swe
**Location:** "D · 🔬 Prove It Works", block 1 (lines 216–224)
**needsRun:** true
**Issue:** The Maven lines contain `…` elisions and appended annotations like "(UNCHANGED — DLT still works)" inside the code fence — not what Surefire prints. The counts are plausible (7 tests match the classes at `step-25-end`), but the contract requires real pasted output, and a learner diffing their console against this block sees a mismatch.
**Fix:** Re-run `./mvnw -pl services/notification test` at `step-25-end` and paste the unedited `Tests run: ...` summary lines (including elapsed times); move the UNCHANGED/NEW annotations out of the fence into a bullet list below the block.

### F8: Starting Point lacks the checkout command and verification

**Severity:** low
**Lens:** structure
**Location:** "📦 Your Starting Point" (lines 169–171)
**needsRun:** false
**Issue:** It states `step-25-start == step-24-end` but gives no command — the contract expects the starting point to anchor on the `step-25-start` tag with an actionable check.
**Fix:** Add: "Continue from your Step-24 working tree, or `git checkout step-25-start`. Verify: `git describe --tags` → `step-25-start` (or `step-24-end` — they point at the same commit)."

### F9: Cross-step references without micro-recaps tax working memory

**Severity:** medium
**Lens:** pedagogy
**Location:** "Security Lens & Thread-safety note" (line 135), "Before You Start" (line 94), Sub-step 2 (line 177)
**needsRun:** false
**Issue:** The lesson leans on Step 20 (the consumer's original shape), Step 21 (DLT + the "Redis pattern"), and Step 7 (proxies/DI) without one-line recaps. A learner returning after weeks must open three old lessons to follow "the parser throws on a poison payload... so it still routes to the DLT (Step 21)".
**Fix:** At first mention of each, add a one-line micro-recap: Step 20 — "the consumer that turns Kafka `transfer.completed` events into SSE pushes"; Step 21 — "poison messages that keep failing are routed to a Dead-Letter Topic after retries, so the listener must let exceptions propagate"; Step 7 — "Spring injects constructor dependencies by type, so a single `@Component` implementing the port is auto-wired".
