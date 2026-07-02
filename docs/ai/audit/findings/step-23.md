# Step 23 audit - swe:4 pedagogy:3 adhd:2 structure:5 - thinBuild:true

## Strengths

- Orient is complete and high-signal: 30-seconds table with effort/what-to-run/tier, skip-test, cheat card with a headline flow diagram, and an explicit "Depends on: Steps 15, 21, 17, 8" line.
- Understand is technically accurate and well-connected to prior steps, with strong misconception handling (compensation is not a rollback; the flow is not isolated; a synchronous orchestrator is not crash-safe) and correct Spring idioms (@HttpExchange over HttpServiceProxyFactory/RestClient, RestClient throwing on 4xx/5xx, Jackson 3 annotation package note).
- Prove includes a §12.3 mutation sanity-check with a realistic failing assertion (`Expecting actual: [] to contain exactly: [42]`) and an honesty note separating stub-HTTP evidence from the live multi-service run.

## Missing spine

- Build sub-step micro-anatomy for ALL 3 sub-steps: exact file locations, complete code (file-path headers, imports, diff view for the CIF edit), line-by-line explanation, per-sub-step under-the-hood, run-and-see with expected output, checkpoint, per-sub-step commit, pitfall. Sub-steps 1 and 2 are goal-only stubs (1-3 lines); only sub-step 3 has a predict and the whole build shares one commit.
- What-we-will-build Mermaid diagram at the top of the Build movement (the files-we-will-touch tree exists in the B→C bridge; the diagram does not — the only flowchart lives in Understand).
- Closing sequence diagram of the flow built (Build ends at "The Finished Result" with no diagram).
- Analogy in the Big Idea (contract: big idea + diagram + analogy; only the first two are present).
- Inline flashcards (3-5) — Review (g) only points at `docs/flashcards.md`; no cards are in the lesson.
- Definition-of-Done checklist — present only as a run-on sentence, not a checkable list.
- Interactivity toolkit in the build: no you-are-here markers, no knowledge-checks, no type-it-yourself, no break-it-on-purpose (one predict and one "little experiments" line are all there is).

## Findings

### F1: The entire build is a stub — three goal-only sub-steps, zero code

**Severity:** high
**Lens:** swe
**Location:** "C · 🛠️ Let's Build It" — Sub-steps 1-3 (lines 187-201)
**needsRun:** true
**Issue:** The lesson promises a new 10+-file service (application class, 2 client interfaces, 4 records, factory, config, orchestrator, exception, controller, application.yml, root-pom registration) plus a CIF service+controller change, but the build contains no code whatsoever — each sub-step is a 1-3 line goal summary. A nervous beginner at 11pm cannot produce `HttpInterfaceClients.create(...)`, the `@HttpExchange` interfaces, token forwarding via `@RequestHeader`, or the 502 error mapping from these hints. Effort says ~12h; 3 stub sub-steps cannot carry that.
**Fix:** Rewrite the build as 6-8 full sub-steps (suggested: 1 CIF deactivate service+controller with diff view; 2 onboarding module skeleton + root `pom.xml` registration + application.yml; 3 request/response records; 4 `CifClient`/`AccountClient` interfaces; 5 `HttpInterfaceClients` factory + `ClientConfig` timeouts; 6 `OnboardingService` + `OnboardingFailedException`; 7 `OnboardingController` + error handling; 8 tests). Each with the complete micro-anatomy: goal, exact file path, complete compiling code with file-path header comment and all imports, line-by-line explanation, under-the-hood, predict-then-run, run-and-see with a real command and truthfully captured output plus a common-wrong-output, checkpoint, commit, pitfall. Run every command to capture output — do not invent it.

### F2: Prove runs tests the learner never wrote

**Severity:** high
**Lens:** swe
**Location:** "D · 🔬 Prove It Works" items 1-2 (lines 226-249) vs the Build movement
**needsRun:** true
**Issue:** The Verification Log's evidence rests on `OnboardingOrchestrationTest` (in-process stub services, `deactivateCalls == [42]`), `OnboardingControllerTest`, and the cif `CustomerControllerTest` deactivate case — none of which appear anywhere in the Build. The learner cannot reproduce the log, and the mutation check in item 2 references a test at line 89 of a file that was never shown.
**Fix:** Add a build sub-step (per F1's sub-step 8) that writes all three tests in full — including the Step-15-style in-process stub servers and the recorded `deactivateCalls` list — with a run-and-see (`./mvnw -pl services/onboarding test` and `./mvnw -pl services/cif test -Dtest=CustomerControllerTest`) whose pasted output matches what Prove shows. Re-run to capture real output.

### F3: No session plan for a ~12-hour step

**Severity:** high
**Lens:** adhd
**Location:** "A · 🧭 Orient" — after "This Step in 30 Seconds" (line 42)
**needsRun:** false
**Issue:** Effort is stated as ≈12 hours focused, but there is no sitting plan, no named save points, and no re-entry guidance anywhere. An ADHD learner has no way to slice this into sessions or to resume mid-build.
**Fix:** Add a "🗓️ Suggested sittings" table after the 30-seconds table: 4-6 sittings of ~2-3h, each naming what is working at its end (e.g., Sitting 1: Orient+Understand + CIF deactivate endpoint green; Sitting 2: clients + factory compile and unit-test; Sitting 3: orchestrator + controller; Sitting 4: tests + mutation check; Sitting 5: smoke + clean-room + play-with-it). At each sitting boundary in the build, add a re-entry line: "Stopping here? You have X working; next session starts at Sub-step N, first action: …".

### F4: No formative checkpoints, knowledge-checks, or scaffold fading in the build

**Severity:** high
**Lens:** pedagogy
**Location:** "C · 🛠️ Let's Build It", Sub-steps 1-3 (lines 187-201)
**needsRun:** false
**Issue:** The build has exactly one predict-then-run (sub-step 3) and zero checkpoints, so none of the four stated objectives is aligned to any formative check during the build; there is no type-it-yourself, so scaffolding never fades — the lesson jumps from fully-summarized to nothing.
**Fix:** When expanding per F1, add: a knowledge-check after the clients sub-step ("Why does the response record need @JsonIgnoreProperties?"), a predict before the timeout config ("What happens today if demand-account hangs for 60s?"), and make the later sub-steps (controller, second test) type-it-yourself with a named pattern to copy from an earlier sub-step ("mirror CifClient: write AccountClient yourself; check against the fold-out"). End each sub-step with an explicit ✅ checkpoint tied to one objective.

### F5: No time-boxes per movement or sub-step

**Severity:** medium
**Lens:** adhd
**Location:** Movement headings A-F and Sub-steps 1-3
**needsRun:** false
**Issue:** Only the whole-step effort (≈12h) is given. No movement or sub-step carries a time estimate, so a learner cannot tell whether being 40 minutes into Sub-step 2 is normal or a signal they are stuck.
**Fix:** Add a time-box to each movement heading (e.g., "A · Orient — ~20 min", "B · Understand — ~1h", "C · Build — ~8h", "D · Prove — ~1.5h", "E/F — ~1h") and to each build sub-step heading ("Sub-step 3 of 8 — the orchestrator (~75 min)").

### F6: No visible-progress markers or re-entry anchors in the build

**Severity:** medium
**Lens:** adhd
**Location:** "C · 🛠️ Let's Build It" — all sub-step headings
**needsRun:** false
**Issue:** Sub-steps are numbered but not "X of N"; there are no you-are-here markers between sub-steps and no "you now have / next up" summaries, so mid-build progress is invisible and every interruption costs a full re-read.
**Fix:** Retitle sub-steps "Sub-step X of N — <name>" and insert a one-line you-are-here marker between sub-steps: "📍 You are here: CIF can deactivate ✅ → clients ⬜ → orchestrator ⬜ → controller ⬜ → tests ⬜", doubling as the re-entry anchor required by F3.

### F7: Play With It contains no runnable commands

**Severity:** medium
**Lens:** swe
**Location:** "🎮 Play With It" (lines 203-212)
**needsRun:** true
**Issue:** The section says "Run auth + cif + demand-account + onboarding" but gives no commands to start four services, and its code block is only comments describing outcomes (201/502) — nothing the learner can execute or compare against. The "little experiments" reference an `ACCOUNT_URL` env var never established anywhere in the lesson.
**Fix:** Give the exact per-terminal start commands (e.g., `./mvnw -pl services/auth spring-boot:run` etc., with ports), the token-acquisition curl, the onboarding curl with a real captured 201 response body, the stop-demand-account/502 sequence with real output, and show where `ACCOUNT_URL`/`services.account.url` is overridden. Run the flow to capture the outputs.

### F8: Build lacks its opening what-we-will-build diagram and closing sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** "B→C bridge" (line 162) and "🏁 The Finished Result" (line 214)
**needsRun:** false
**Issue:** The contract requires the Build to open with a what-we-will-build Mermaid diagram plus the files tree, and to close with a sequence diagram of the flow built. Only the files tree exists; the sole flowchart sits back in Understand, and the build ends with no diagram.
**Fix:** Add a Mermaid flowchart of the target architecture (controller → OnboardingService → CifClient/AccountClient → services, with the compensation edge) directly above the files tree in the B→C bridge, and add a `sequenceDiagram` (caller → onboarding → CIF create → account open [Authorization forwarded] → alt failure: CIF deactivate → 502 / success: 201) just before "The Finished Result".

### F9: Flashcards are not in the lesson

**Severity:** medium
**Lens:** structure
**Location:** "F · 🏆 Review" item (g) (line 319)
**needsRun:** false
**Issue:** The recap only says flashcards were "appended to docs/flashcards.md"; the contract requires 3-5 flashcards present in the lesson.
**Fix:** Inline 4 Q→A flashcards under (g), e.g.: "Orchestration vs choreography?"; "Why compensation instead of rollback?"; "How does the orchestrator authenticate to demand-account?"; "Why isn't the synchronous orchestrator crash-safe?" — keeping the pointer to docs/flashcards.md as a secondary line.

### F10: Big Idea has no analogy

**Severity:** low
**Lens:** pedagogy
**Location:** "🧠 The Big Idea — someone has to coordinate" (lines 105-121)
**needsRun:** false
**Issue:** The contract's Understand movement is big idea + diagram + analogy; the diagram is there but no concrete real-world analogy anchors orchestration vs choreography for a beginner.
**Fix:** Add two sentences after the bullet pair: an orchestrator is a wedding planner (one person calls the caterer, then the florist, and cancels the caterer if the florist falls through); choreography is a dance troupe (no caller — each dancer reacts to the others' moves).

### F11: Glossary is a term list with no definitions

**Severity:** low
**Lens:** pedagogy
**Location:** "📚 Learn More & Glossary" (line 303)
**needsRun:** false
**Issue:** The glossary line only enumerates seven terms (orchestration, choreography, compensating transaction, …) without defining any of them, so it cannot serve look-up during the build.
**Fix:** Convert to a bullet list with a one-line definition per term (e.g., "*compensating transaction* — a new forward action that semantically undoes a previously committed step, used because committed remote steps cannot be rolled back").

### F12: Definition of Done is a sentence, not a checklist

**Severity:** low
**Lens:** structure
**Location:** "🏁 The Finished Result" (line 216)
**needsRun:** false
**Issue:** The DoD is one run-on bold sentence; the contract requires a checkable Definition-of-Done checklist the learner can tick.
**Fix:** Reformat as checkboxes: `- [ ] POST /api/onboarding returns 201 with customerNumber+accountNumber` / `- [ ] forced account-open failure returns 502 and the customer is KYC REJECTED` / `- [ ] ./mvnw verify green (13 modules)` / `- [ ] bash steps/step-23/smoke.sh passes` / `- [ ] committed and tagged step-23-end`.
