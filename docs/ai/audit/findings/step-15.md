# Step 15 audit - swe:7 pedagogy:6 adhd:4 structure:8 - thinBuild:true

## Strengths
- Verification Log is exemplary in honesty: stated 🔴 Full tier, a real §12.3 mutation with pasted failure output, clean-room check, and an explicit honesty note distinguishing the stub-based automated proof from the manual full front-to-back path.
- Technically accurate and current throughout: servlet-vs-reactive gateway rationale tied to virtual threads (Step 11), the `spring.cloud.gateway.server.webmvc.*` prefix migration called out, correct `@HttpExchange`/`HttpServiceProxyFactory`/`RestClient`/`JdkClientHttpRequestFactory` idioms, and every deferred concern named with its future step (16-17/20/37/51).
- Consistent micro-scaffolding: you-are-here markers on every sub-step heading, checkpoint + commit + pitfall on all six sub-steps, and a retrieval set (5 test-yourself + 5 flashcards) that targets the step's core ideas rather than trivia.

## Missing spine
- Complete, compiles-as-shown code for most sub-steps (0, 2, 3, 4, 5 are fragments or "essence; full file in the repo")
- `gateway/pom.xml` (full file), `GatewayApplication.java`, and the root `pom.xml` `<module>` diff are never shown at all
- Run-and-see (exact command + expected output) for sub-steps 0, 1, 3, 4
- Common-wrong-output blocks (absent from all sub-steps, including the two that have run-and-see)
- Predict-then-run for sub-steps 0, 2, 3, 4, 5 (only sub-step 1 has one)
- Session plan with named save points for an ~18-hour step
- Re-entry lines at checkpoints ("stopping here? next session starts at...")
- Per-movement and per-sub-step time-boxes
- Type-it-yourself / scaffold fading in later sub-steps
- Glossary definitions (terms are listed with no definitions)

## Findings

### F1: Build code is fragments; a beginner cannot type this lesson into a working system
**Severity:** high
**Lens:** swe
**Location:** C · Build, sub-steps 0-5 (lines ~249-486)
**needsRun:** false
**Issue:** The sacred-build contract requires complete code (file-path header comment, all imports, package line, compiles as shown; never fragments). Sub-steps 2 and 5 say "the essence; full file in the repo" and contain literal `throws ...` and a `/* respond 200 {"ok":true} */` comment in place of the `HttpServer` response-writing code (headers + body + close the exchange — exactly the part a beginner will get wrong, causing the forward to hang). Sub-step 3 lacks package lines and imports for `@HttpExchange`/`@GetExchange`/`@PathVariable`. Sub-step 4 shows a bare static method and a bare `@Bean` method with no enclosing class declarations or imports. Sub-step 0 shows one `<dependency>` block, not a module POM. A nervous beginner at 11pm cannot complete this build without opening the repo's solution files.
**Fix:** Replace each fragment with the complete file from the already-verified repo: full `GatewayRoutingTest.java` (including the stub's response-writing lines and all imports), full `CifClient.java`/`CifCustomer.java` with package + imports, full `CifClientFactory.java` and `CifClientConfig.java` as whole classes, full `CifClientTest.java` (including the stub setup and the `/slow` sleep handler). Add a `// file: <path>` header comment to every code block, including the sub-step 1 `application.yml` block.

### F2: Run-and-see missing for 4 of 6 sub-steps; no common-wrong-output anywhere
**Severity:** high
**Lens:** structure
**Location:** C · Build, sub-steps 0, 1, 3, 4 (and the two existing run-and-sees in sub-steps 2 and 5)
**needsRun:** true
**Issue:** The micro-anatomy requires run-and-see (exact command + expected output + common-wrong-output) for EVERY sub-step. Sub-steps 0, 1, 3, 4 have checkpoints ("compiles", "routes are defined") but no command output the learner can compare against; sub-step 0's `dependency:resolve` checkpoint shows no expected output. The two run-and-sees that exist (sub-steps 2, 5) lack common-wrong-output blocks. The first expected output a learner can compare against arrives only in sub-step 2, after writing the POM, root-POM edit, application class, YAML, and a full test — so the first win is late, and the reward loop across the build is 2 runs total.
**Fix:** Add a run-and-see to sub-steps 0 (`./mvnw -q -pl gateway dependency:resolve` with real output), 1 (`./mvnw -pl gateway compile` or boot the gateway and curl a route with real output), 3 and 4 (`./mvnw -pl services/demand-account -am compile` or run the existing test subset), each with genuinely captured output plus one common-wrong-output (e.g. for sub-step 1: the 404-everything symptom of the deprecated `spring.cloud.gateway.mvc.routes` prefix). Do not fabricate output — run the commands and paste.

### F3: No session plan or re-entry support for an 18-hour step
**Severity:** high
**Lens:** adhd
**Location:** A · Orient (30-seconds table, line ~35) and all Build checkpoints
**needsRun:** false
**Issue:** The step declares "≈ 18 hours focused" but contains no sitting plan (contract: 6-10 sittings of ~2-3h with named save points for a step this size) and no re-entry lines at any checkpoint. An ADHD learner returning after a break has no "you have X working; next session starts at sub-step N, first action: ..." anywhere — every commit point is a silent stop.
**Fix:** Add a "Suggested sittings" box at the end of Orient, e.g. Sitting 1: Understand + sub-step 0 (~2.5h, save point: gateway module resolves); Sitting 2: sub-steps 1-2 (~3h, save point: routing test green); Sitting 3: sub-steps 3-5 (~3h, save point: client tests green); Sitting 4: Play With It + Prove + smoke (~2.5h); Sitting 5: Apply + Review (~2h). Then append one re-entry line to each sub-step checkpoint: "Stopping here? You have <artifact> working and committed; next session starts at sub-step N — first action: open <file>."

### F4: Lesson omits the actuator dependency and exposure config its own instructions rely on
**Severity:** medium
**Lens:** swe
**Location:** Sub-step 0 (pom snippet), sub-step 1 (`application.yml`), 🎮 Play With It item 3, 🛡️ Security Lens bullet 4
**needsRun:** false
**Issue:** Play With It says "See the routes: `GET http://localhost:8080/actuator/gateway/routes`" and the Security Lens warns about that endpoint leaking topology — but the lesson's pom snippet shows only the gateway starter and its `application.yml` has no `management.endpoints.web.exposure` block. The actual repo's `gateway/pom.xml` includes `spring-boot-starter-actuator` and its `application.yml` exposes `health,info,gateway`; a learner who types only what the lesson shows gets a 404 on that URL.
**Fix:** In sub-step 0, show the full pom including `spring-boot-starter-actuator` (per F1); in sub-step 1, append the repo's real `management: endpoints: web: exposure: include: health,info,gateway` block to the YAML with a one-line explanation ("exposes the `gateway` actuator endpoint that lists routes — gate it in production, see Security Lens").

### F5: No time-boxes below the whole-step level
**Severity:** medium
**Lens:** adhd
**Location:** Movement headings A-F and sub-step headings 0-5
**needsRun:** false
**Issue:** The only time signal is the step-level "≈ 18 hours" (which is also hard to reconcile with a 6-sub-step, ~200-line build — undecomposed estimates read as arbitrary and demotivating). No movement or sub-step carries an expected duration, so a learner cannot plan a sitting or know they're behind. Go Deeper optional items also carry no time cost.
**Fix:** Add "(~Xh)" to each movement heading (e.g. "B · Understand (~3h)", "C · Build (~8h)") and "(~40 min)"-style boxes to each sub-step heading; label each Go Deeper `<details>` with "(optional, ~15 min)". Make the sum visibly reconcile with the Orient estimate, adjusting the 18h figure if the decomposition doesn't support it.

### F6: Interactivity toolkit is thin — one predict, no mid-build knowledge checks, no scaffold fading
**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, sub-steps 0, 2, 3, 4, 5
**needsRun:** false
**Issue:** Of the contract's toolkit, only sub-step 1 has a predict-then-run and only sub-step 2 has a break-it exercise; there are no knowledge checks inside the build and no type-it-yourself progression — every sub-step is fully worked (to the extent the fragments are worked at all), so scaffolding never fades.
**Fix:** Add a predict before each run/checkpoint: sub-step 2 ("if the stub returns 500, what status does the client see through the gateway?"), sub-step 4 ("the bean is created at startup — does it open a connection? "), sub-step 5 ("which test fails if you swap the 400ms and 1.5s values?"). Convert sub-step 5 to partial type-it-yourself: give the deserialize test worked, then ask the learner to write the timeout test themselves from the sub-step 4 concepts, with the solution in a collapsed `<details>`.

### F7: Glossary is a term list with no definitions
**Severity:** medium
**Lens:** pedagogy
**Location:** F · 📚 Learn More: Resources & Glossary (line ~641)
**needsRun:** false
**Issue:** The glossary is a single run-on line of ~14 bolded terms separated by "·" with zero definitions — it cannot serve lookup during the build or review, and as a wall of jargon it adds load instead of relieving it.
**Fix:** Reformat as a definition list, one line per term: "**Route** — id + target uri + predicates + filters; the gateway's unit of forwarding." etc., for each of the 14 terms, reusing the one-liners already written in Understand.

### F8: Config-property and shell-syntax inconsistencies will trip learners
**Severity:** low
**Lens:** swe
**Location:** Cheat Card commands (lines ~62-74), sub-step 1 YAML (`services.cif.uri`) vs sub-step 4 bean (`services.cif.url`)
**needsRun:** false
**Issue:** The gateway route uses `${services.cif.uri:...}` while the demand-account client uses `${services.cif.url:...}` — same conceptual setting, two spellings, no acknowledgment; a learner wiring config will set the wrong key in the wrong module. Separately, the Cheat Card's `SPRING_DATASOURCE_URL=... ./mvnw ...` inline env-var prefix is bash-only, yet the card's own note says "Windows uses `.\mvnw.cmd`" — on PowerShell that line fails to parse.
**Fix:** Add a one-line callout in sub-step 4: "note: the gateway names its property `services.cif.uri`, the client `services.cif.url` — they live in different modules; watch the spelling" (or unify the names in a follow-up). In the Cheat Card, add the PowerShell equivalent: `$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5433/demand_account'; .\mvnw.cmd -pl services/demand-account spring-boot:run`.

### F9: "Choose sync vs async" objective has no aligned retrieval item
**Severity:** low
**Lens:** pedagogy
**Location:** ✅ What You'll Be Able to Do (bullet 5) vs 🧠 Test Yourself (c) and Flashcards (g)
**needsRun:** false
**Issue:** Every other outcome maps to a test, checkpoint, or test-yourself question, but the sync-vs-async objective appears only in the skip-test and interview prep — neither is the contract's checkpoint/exercise/test-yourself alignment, so it's never practiced or retrieved within the step.
**Fix:** Add Test Yourself Q6: "You need the answer before you can proceed vs. you can tolerate eventual consistency — which communication style fits each, and what's the coupling trade-off?" and a sixth flashcard "Q: Sync HTTP vs async messaging? · A: immediate but temporally coupled vs decoupled but eventually consistent (Kafka, Step 20)."

### F10: Sub-step numbering is zero-indexed against a denominator of 5 for six sub-steps
**Severity:** low
**Lens:** structure
**Location:** Sub-step headings, lines ~249, 275, 326, 380, 409, 448
**needsRun:** false
**Issue:** Headings run "Sub-step 0 of 5" through "Sub-step 5 of 5" — six sub-steps denominated as five. Progress markers exist to reduce uncertainty; "0 of 5" then "5 of 5" makes the learner recount how much is left.
**Fix:** Renumber to "Sub-step 1 of 6" ... "Sub-step 6 of 6" (updating the build-overview Mermaid node labels and the §12.3/"See 🔬 §3" cross-references to match), or keep zero-indexing but write "of 6".
