# Step 13 audit - swe:7 pedagogy:7 adhd:4 structure:8 - thinBuild:false

## Strengths

- The Verification Log is genuinely evidential: tier stated (Full), real pasted outputs including live HTTP headers with actual UUIDs, the section-12.3 mutation (422 -> 200 failure) and a clean-room re-clone -- nothing reads as invented.
- Filter-vs-interceptor teaching is technically precise and honest about sharp edges: why postHandle can't set headers after commit, why @WebMvcTest slices load filters/WebMvcConfigurers, and thread-safety tied back to Step 11 with the request-attribute pattern.
- Strong advance organizers and continuity: embassy analogy, two aligned Mermaid diagrams (cheat-card flow + closing sequence diagram), and the "cash that cheque" callback to Step 12's placeholder ApiExceptionHandler.

## Missing spine

- Session plan with named save points (contract requirement for an ~18h step) -- absent from Orient
- Per-movement and per-sub-step time-boxes -- only the whole-step estimate exists
- Re-entry lines at checkpoints ("stopping here? next session starts at...") -- absent everywhere
- Type-it-yourself -- absent from all 7 build sub-steps (no scaffold fading)
- Break-it-on-purpose -- absent from the build (the mutation exists only as authored evidence in Movement D)
- Predict-then-run missing in sub-steps 0, 2, 3, 5; run-and-see missing in sub-steps 0, 1, 4

## Findings

### F1: Java snippets are not complete-as-shown (no package lines, no imports, no file-path headers)

**Severity:** high
**Lens:** swe
**Location:** Build sub-steps 1-5 (GlobalExceptionHandler, OpenApiConfig, RequestIdFilter, TimingInterceptor, WebConfig), lines 303-506
**needsRun:** false
**Issue:** Every Java code block violates the "complete code: file-path header comment, all imports, compiles as shown" contract. GlobalExceptionHandler alone needs ~8 imports (ProblemDetail, HttpStatus, URI, RestControllerAdvice, ResponseEntityExceptionHandler, ExceptionHandler, the domain exception); OpenApiConfig needs io.swagger.v3.oas.models.info.Info (the lesson's line-by-line says only "from io.swagger.v3.oas.models", which is the wrong package for Info). A beginner typing these in gets a wall of compile errors with no guidance.
**Fix:** For each of the five Java blocks, prepend a `// services/demand-account/src/main/java/com/buildabank/account/web/<Name>.java` header comment, the `package com.buildabank.account.web;` line, and the full import block copied from the actual repo files. Update sub-step 3's line-by-line to name `io.swagger.v3.oas.models.info.Info` exactly.

### F2: Sub-step 2's Run & See is untrue at that point in the sequence

**Severity:** high
**Lens:** swe
**Location:** Sub-step 2 "Run & See", lines 372-377 (vs sub-step 6 pitfall, line 583)
**needsRun:** true
**Issue:** Sub-step 2 tells the learner to run `./mvnw -pl services/demand-account test -Dtest=TransferControllerTest` and promises a green validation-ProblemDetail result. But the updated tests only arrive in sub-step 6 -- sub-step 6's own pitfall admits the Step-12 tests assert the old `{"error":...}` shape, so at sub-step 2 this command fails (old assertions break against the new handler), and the promised validation test does not exist yet. Sub-steps 1-5 also commit on a red test suite without saying so.
**Fix:** Either (a) move the TransferControllerTest assertion updates forward into sub-steps 1-2 so each commit is green, re-running to capture truthful output; or (b) replace this Run & See with an honest note ("the Step-12 slice tests are RED from now until sub-step 6 -- expected; here's the failure you'll see") plus the real failing output, and move the green run to sub-step 6. Either way, run the commands and paste actual output.

### F3: 18-hour step with no session plan or named save points

**Severity:** high
**Lens:** adhd
**Location:** Orient, "This Step in 30 Seconds" table (line 35) and Before You Start (lines 104-118)
**needsRun:** false
**Issue:** The step declares ~18 hours of focused effort but plans zero sittings. The contract requires 6-10 planned sittings of ~2-3h with named save points for a step this size. An ADHD learner has no way to see "where can I safely stop tonight and what do I lose."
**Fix:** Add a "Session plan" block at the end of Orient: e.g. S1 Orient + Understand (lifecycle/ProblemDetail theory, ~2.5h); S2 Understand rest + sub-steps 0-1 (~2.5h, save point = ProblemDetail commit); S3 sub-steps 2-3 (~3h, save point = Swagger UI live); S4 sub-steps 4-5 (~2.5h, save point = headers curl); S5 sub-step 6 + Prove (~3h, save point = 13 green + tag); S6 Apply + Review (~2.5h). Name each save point after its existing commit message.

### F4: No re-entry support at any checkpoint

**Severity:** high
**Lens:** adhd
**Location:** All 7 sub-step checkpoints (lines 288, 336, 378, 424, 462, 527, 579)
**needsRun:** false
**Issue:** Every checkpoint says what should be true but never supports stopping/resuming. A learner returning after three days has no "you have X working; next session starts at sub-step N, first action: ..." line and must re-read the build to find their place.
**Fix:** Append one re-entry line to each checkpoint, e.g. after sub-step 3: "Stopping here? You have ProblemDetail errors + a live Swagger UI, committed. Next session: sub-step 4 (RequestIdFilter); first action: create `RequestIdFilter.java` in the `web/` package."

### F5: No time-boxes below whole-step granularity

**Severity:** medium
**Lens:** adhd
**Location:** Movement headings A-F and all sub-step headings (lines 27, 124, 229, 267-535)
**needsRun:** false
**Issue:** Only the global "~18 hours" exists. No movement or sub-step carries an estimate, so a learner cannot budget a sitting or notice they're stuck (10 minutes over a 20-minute box is a signal; over an 18-hour box it's invisible).
**Fix:** Add "(~X min)" to each movement heading (e.g. "B - Understand (~3h)") and each sub-step heading (e.g. "Sub-step 1 of 6 -- GlobalExceptionHandler (~45 min)"), summing to the stated 18h. Also label each Go Deeper item and Your Turn challenge with its time cost.

### F6: Sub-step 6 teaches the tests as fragments the learner cannot write

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-step 6, code blocks at lines 541-565
**needsRun:** false
**Issue:** The test sub-step shows only floating `.andExpect(...)` chains and two assertion lines with "full files in the repo". The learner must take the suite from 11 to 13 tests but is never shown a complete new test method, where the assertions live, which two tests are new, or the new imports -- a direct "never fragments" contract violation on the sub-step that gates the step's Definition of Done.
**Fix:** Replace the fragments with the two complete new/changed test methods (full method signature, arrange/act/assert, class context comment) plus a diff-style view of the modified assertions in the existing tests, and one line naming the delta explicitly: "TransferControllerTest 4->5 (new: validationReturnsProblemDetail400), DemandAccountIntegrationTest 1->2 (new: openApiDocServed)" -- matching the per-class counts already printed in Prove section 1.

### F7: No type-it-yourself anywhere -- scaffolding never fades

**Severity:** medium
**Lens:** pedagogy
**Location:** Build sub-steps 0-6 (lines 267-583)
**needsRun:** false
**Issue:** All seven sub-steps are fully worked examples (or fragments). The contract expects later sub-steps to shift toward type-it-yourself; by sub-step 5 the learner has seen the OncePerRequestFilter pattern and could write the interceptor from a spec, but is handed the code again -- no retrieval during the build.
**Fix:** Convert sub-step 5 (TimingInterceptor + WebConfig) into guided type-it-yourself: give the class skeleton, the three method signatures, and a 4-bullet spec ("store nanoTime in a request attribute named timing.startNanos; set X-Timing-Enabled in preHandle; log method/URI/status/ms in afterCompletion; register via WebMvcConfigurer"), with the full solution in a collapsed `<details>` block.

### F8: Break-it-on-purpose exists only as authored evidence, not as a learner activity

**Severity:** medium
**Lens:** pedagogy
**Location:** Prove section 5 (lines 674-681); absent from Build
**needsRun:** false
**Issue:** The section-12.3 mutation (UNPROCESSABLE_ENTITY -> OK -> test fails -> revert) is narrated as something the author did. The build itself never asks the learner to break anything, losing the contract's break-it-on-purpose interactivity and the confidence that *their* tests are load-bearing.
**Fix:** Add a "Break it on purpose" box at the end of sub-step 6 instructing the learner to make the same one-line mutation, run `./mvnw -pl services/demand-account test -Dtest=TransferControllerTest`, compare against the expected failure already pasted in Prove section 5, and revert (`git checkout -- <file>`).

### F9: Sub-step 5's curl silently depends on state from earlier sub-steps

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 5 "Run & See", lines 515-525
**needsRun:** false
**Issue:** The curl POSTs a transfer between ACC-A and ACC-B and shows a success-shaped output, but never says the spring-boot:run server from sub-step 3 must still be running, nor that ACC-A/ACC-B must first be created (account creation is only mentioned later in Play With It). A learner running exactly what's shown against a fresh DB gets an error response and thinks the sub-step failed -- even though both headers would actually still be present.
**Fix:** Before the curl, add: "Server from sub-step 3 still running? If not, restart it. Create the two accounts first (requests.http, first two requests, or the Play-With-It Swagger flow)." Add one sentence after the expected output: "Both headers appear even if the transfer itself errors -- the filter and interceptor run regardless."

### F10: Micro-anatomy gaps -- run-and-see missing in sub-steps 0, 1, 4; predicts missing in 0, 2, 3, 5

**Severity:** medium
**Lens:** structure
**Location:** Sub-steps 0 (lines 267-292), 1 (296-341), 4 (432-466); predicts absent in 0, 2, 3, 5
**needsRun:** true
**Issue:** Three of seven sub-steps have no run-and-see (sub-step 0's checkpoint has a command but no expected output; sub-step 1's checkpoint is just "compiles"; sub-step 4's checkpoint "responses carry X-Request-Id" gives no command at all -- the learner can't verify the filter until sub-step 5). Predict-then-run is missing from four sub-steps. Also, headers say "Sub-step 0 of 6" through "6 of 6" for seven sub-steps -- relabel as "of 7" or "1 of 7".."7 of 7".
**Fix:** Add a run-and-see with real captured output to sub-steps 0 (dependency:resolve output tail), 1 (`./mvnw -q -pl services/demand-account -am compile` result), and 4 (restart the app, curl any endpoint, grep X-Request-Id). Add one-line predicts to sub-steps 0, 2, 3, 5. Fix the sub-step count labels. The new expected-output blocks require actual runs -- do not invent them.

### F11: Error detail leaks the account balance -- contradicts the lesson's own Security Lens

**Severity:** low
**Lens:** swe
**Location:** Prove section 3 (line 661): `"detail":"account ACC-A balance 150.0000 < debit 9999.00"`; Security Lens (line 204)
**needsRun:** true
**Issue:** The Security Lens says keep `detail` user-safe and don't reveal internals, yet the shipped 422 body discloses the account's exact balance to any caller. Pre-auth (Step 16 hasn't happened), anyone who can POST a transfer can enumerate balances via crafted overdraws -- a real banking data-exposure pattern the course should model correctly.
**Fix:** Change the InsufficientFundsException message (or the advice's detail mapping) to omit the balance, e.g. "insufficient funds on ACC-A for debit 9999.00"; re-run the overdraw flow and the slice test and paste the new real output into Prove section 3; add one sentence to the Security Lens using the old-vs-new detail as the worked example of balance non-disclosure.

### F12: First visible win comes too late; sub-steps 0-1 give no observable reward

**Severity:** low
**Lens:** adhd
**Location:** Sub-steps 0-1 (lines 267-341) vs sub-step 3 (first live payoff, line 411)
**needsRun:** false
**Issue:** The first ~2 sub-steps produce only "dependency downloads" and "it compiles" -- the first thing the learner *sees working* (Swagger UI / a ProblemDetail over HTTP) arrives at sub-step 3, likely 1-2 hours into the build. Contract wants something visibly running within ~10 minutes.
**Fix:** At the end of sub-step 0, add a 5-minute "instant win": start the app (compose + spring-boot:run, commands already in the cheat card) and open /swagger-ui.html -- springdoc serves a default UI with zero config, before any Java is written. Frame sub-step 3 as "now make the metadata yours". Reuse the already-verified sub-step 3 commands; no new claimed output needed.
