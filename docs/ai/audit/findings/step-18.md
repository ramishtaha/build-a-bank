# Step 18 audit - swe:7 pedagogy:6 adhd:4 structure:6 - thinBuild:true

## Strengths

- Exceptionally honest security teaching: the lesson surfaces a real critical BOLA bug in the course's own code and models senior behavior (risk register + scheduled fix) instead of hand-waving it; the CORS two-beans pitfall is a genuine, verbatim war story from building the step.
- The Verification Log is strong: stated Full tier, real pasted Surefire output with before/after test counts, and a genuine mutation check (permissive CORS makes the deny-by-default test fail with pasted failure output, then reverted).
- Misconception handling is excellent: CORS is not an access control, HSTS only over TLS, parameterization vs escaping proven by a vulnerable-vs-safe contrast test with opposite results (0 vs 1) on the same payload.

## Missing spine

- Closing sequence diagram of the flow built (build opens with the required mermaid + file tree but never closes with a sequence diagram).
- Run-and-see (exact command + expected output + common-wrong-output) is absent from ALL five build sub-steps; run evidence lives only in the Verification Log.
- Complete code: sub-steps 1, 4, and 5 defer their artifact to "full content in the repo" (threat-model.md, risk-register.md, SecurityHardeningTest.java, SqlInjectionSafetyTest.java are never shown in full).
- Inline flashcards: line 489 defers to `docs/flashcards.md`; the contract requires 3-5 flashcards in the lesson.
- Micro-anatomy gaps: no checkpoint in sub-steps 2, 4, 5; no commit line in sub-steps 3, 4; no pitfall in sub-steps 2, 4; no you-are-here / "Sub-step X of 5" markers.
- ADHD session plan: a ~14-hour step with no planned sittings, no per-movement or per-sub-step time-boxes, and no re-entry lines at checkpoints.
- Definition of Done is a run-on sentence inside "The Finished Result", not a checklist.
- Duplicate anchor: `<a id="build"></a>` appears twice (lines 183 and 213), so the TOC "Build" link lands on the bridge, not the build proper, and the second id is dead.

## Findings

### F1: Core build artifacts deferred to "full content in the repo"

**Severity:** high
**Lens:** structure
**Location:** Sub-steps 1, 4, 5 (lines 223-247, 318-334, 336-358)
**needsRun:** false
**Issue:** The lesson's main deliverables - `security/threat-model.md`, `security/risk-register.md`, `SecurityHardeningTest.java`, and `SqlInjectionSafetyTest.java` - are described but never given. Each says "full content/file in the repo." A nervous beginner at 11pm cannot write a STRIDE threat model, a risk-register entry, or a `@WebMvcTest` slice with mocked `JwtDecoder` from three one-line method signatures. This breaks the sacred-build contract for 3 of 5 sub-steps and is why the build is thin.
**Fix:** Inline the complete files. For sub-step 4 and 5, paste the full test classes from the repo verbatim (package line, all imports, file-path header comment). For sub-step 1, inline the risk-register R-001 entry in full plus the threat-model skeleton (section headings + the trust-boundary table + the complete demand-account STRIDE-per-element table), leaving the remaining tables as guided type-it-yourself work against the repo copy as answer key.

### F2: No run-and-see in any build sub-step

**Severity:** high
**Lens:** structure
**Location:** Entire Build movement (lines 215-358)
**needsRun:** true
**Issue:** Not one sub-step contains the contractual run-and-see (exact command + expected output + common-wrong-output). The learner writes config, a bean, and two test classes across five sub-steps without ever being told to run anything until the Verification Log. Reward-loop density in the build is effectively zero.
**Fix:** After sub-step 4 add a run-and-see: `./mvnw -pl services/demand-account -am test -Dtest=SecurityHardeningTest` with real pasted output and a common-wrong-output (the two-beans context failure). After sub-step 5 add the same for `./mvnw -pl services/cif -am test -Dtest=SqlInjectionSafetyTest` (common-wrong-output: Docker not running). After sub-step 1 add a check command (e.g. `grep -c "R-001" security/risk-register.md`). Actually run the commands to capture truthful output.

### F3: 14-hour step with no session plan, time-boxes, or re-entry support

**Severity:** high
**Lens:** adhd
**Location:** Orient table (line 37) and all checkpoints
**needsRun:** false
**Issue:** Effort is stated as ~14 hours ("about half is thinking") but there is no sitting plan, no per-movement or per-sub-step time budget, and no "stopping here?" re-entry lines. For an ADHD learner, "write a threat model, roughly 7 hours" is a single undifferentiated wall of work with no save points.
**Fix:** Add a "Session plan" box to Before You Start with ~6 sittings and named save points, e.g. S1 (2h) Understand + DFD + trust-boundary table; S2 (2.5h) STRIDE tables for demand-account + gateway; S3 (2.5h) attack trees + OWASP walkthroughs + risk register; S4 (2.5h) sub-steps 2-4 hardening + test; S5 (2h) sub-step 5 + Verification Log; S6 (2.5h) capstone + review. Add a time-box line under each sub-step heading and a re-entry line at each checkpoint ("Stopping here? You have X working; next session starts at sub-step N, first action: ...").

### F4: First win comes hours too late

**Severity:** high
**Lens:** adhd
**Location:** Sub-step 1 (lines 223-247) as the build opener
**needsRun:** true
**Issue:** The build opens with the longest, most abstract task (writing the threat model - potentially several sittings of pure document work) before anything is ever executed. Nothing visibly runs in the first several hours, violating first-win-fast and starving the reward loop exactly where motivation is most fragile.
**Fix:** Insert a 10-minute "Sub-step 0 - see the attack surface live" before the modeling work: start demand-account, curl the current headers (observe what is missing pre-hardening), and perform the BOLA read of another user's account (currently in Play With It, line 378) so the learner experiences the bug the model will formalize. Requires running the pre-hardening state to capture truthful baseline output.

### F5: Flashcards missing from the lesson

**Severity:** high
**Lens:** structure
**Location:** Recap item (g), line 489
**needsRun:** false
**Issue:** The contract requires 3-5 flashcards in the recap; the lesson only points at `docs/flashcards.md`, so the retrieval-practice artifact is absent from the lesson itself.
**Fix:** Inline 4 flashcards under (g): Q: the six STRIDE letters? / Q: what is BOLA and which OWASP API item? / Q: why is deny-by-default CORS not an access control? / Q: how do you actually prevent SQL injection? (keep the pointer to docs/flashcards.md as a secondary line).

### F6: SecurityConfig changes shown as floating fragments without imports or diff view

**Severity:** medium
**Lens:** swe
**Location:** Sub-steps 2-3 code blocks (lines 258-299)
**needsRun:** false
**Issue:** The `.headers(...)` block starts with a dot and has no surrounding method; the CORS bean omits every import it needs (`List`, `Duration`, `@Value`, `CorsConfiguration`, `CorsConfigurationSource`, `UrlBasedCorsConfigurationSource`) and `Customizer` is never import-noted. The contract requires complete code that compiles as shown, with diff view for edits - a learner cannot tell where in `filterChain` these lines go or what to add at the top of the file.
**Fix:** Replace both snippets with a single unified diff (or full after-state) of `SecurityConfig.java` from the repo, including the package line, the complete import block with the six new imports highlighted, and the full `filterChain` method showing exactly where `.headers(...)` and `.cors(...)` sit relative to the existing `oauth2ResourceServer`/`authorizeHttpRequests` calls.

### F7: Cheat-card test command may fail in upstream -am modules

**Severity:** medium
**Lens:** swe
**Location:** Cheat Card, line 66
**needsRun:** true
**Issue:** `./mvnw -pl services/cif,services/demand-account -am test -Dtest='SqlInjectionSafetyTest,SecurityHardeningTest'` builds upstream modules via `-am`; with `-Dtest` set, Surefire fails any module containing tests where the pattern matches nothing ("No tests were executed!"). Whether this bites depends on whether upstream modules (parent, shared libs, starters from Step 28-era layout) contain tests. If it fails, the very first command a learner copies breaks.
**Fix:** Run the command as written on step-18-end. If it fails, change the cheat card to add `-Dsurefire.failIfNoSpecifiedTests=false` (and explain that flag on first appearance) or split into two per-module commands.

### F8: No worked example of the actual STRIDE analysis the learner must produce

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-step 1, "The method, concretely" (lines 229-234)
**needsRun:** false
**Issue:** The learner is told the method ("ask all six letters, record one of three statuses") but shown only the two resulting 🔴 rows. The step's stated highest-leverage skill - performing STRIDE-per-element - has no worked example in the lesson, and there is no scaffold fading (worked example first, then learner does the next element solo).
**Fix:** Inline the complete six-row STRIDE table for the demand-account process (copied from `security/threat-model.md`), with each row's mitigation mapped to the step that built it (S -> Step 17 JWT, T -> Step 9 ledger invariants, etc.), then instruct: "now do the gateway and cif elements yourself; compare against security/threat-model.md sections 5.2-5.3 when done."

### F9: Missing you-are-here markers, checkpoints, and commits across sub-steps

**Severity:** medium
**Lens:** adhd
**Location:** Sub-step headings and tails (lines 223, 250, 276, 318, 336)
**needsRun:** false
**Issue:** No "Sub-step X of 5" progress markers; sub-steps 2, 4, 5 end without a checkpoint; sub-steps 3 and 4 have no commit line. Only two commit points exist for ~7 hours of code work, so a learner who stops mid-way has no visible progress trail and nothing safely saved.
**Fix:** Rename headings to "Sub-step N of 5 - ..."; add a one-line checkpoint to sub-steps 2 (`curl -i` shows the three headers), 4 and 5 (test class green); add a commit line to sub-steps 3 and 4 (e.g. `feat(demand-account): deny-by-default CORS + hardening tests (Step 18)`).

### F10: Build never closes with the contractual sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** End of Build, between Play With It and The Finished Result (lines 380-382)
**needsRun:** false
**Issue:** The contract requires the build to close with a sequence diagram of the flow built. The lesson has the opening what-we-will-build flowchart and file tree but ends the build with prose only.
**Fix:** Add a mermaid `sequenceDiagram` before "The Finished Result" showing two lanes: (1) browser preflight `OPTIONS /api/v1/transfers` with `Origin: https://evil.example` -> CorsFilter consults `corsConfigurationSource` (empty allow-list) -> 403, no ACAO; (2) authenticated `GET /api/accounts/{n}` -> HeaderWriterFilter stamps nosniff/DENY/no-referrer -> BearerTokenAuthenticationFilter -> controller -> 200 with headers.

### F11: Play With It assumes a running service but never says how to start it

**Severity:** low
**Lens:** adhd
**Location:** Play With It (lines 361-378)
**needsRun:** false
**Issue:** "Run the live service and see the defenses" gives curl commands but no start command (Postgres via Docker, `spring-boot:run` for demand-account), and "Log in for a token" has no command or pointer - a distant cross-reference to Step 17 with no micro-recap, a working-memory tax at the exact moment of the payoff demo.
**Fix:** Prepend the two start commands used in Steps 16-17 (e.g. `docker compose up -d` then `./mvnw -pl services/demand-account spring-boot:run`) and a one-line token recap ("get a token as in Step 17: POST /api/auth/login on :8083 - full request in requests.http").

### F12: Verification Log claims clean-room and smoke evidence without pasting it

**Severity:** low
**Lens:** swe
**Location:** Verification Log items 4-5 (lines 426-428)
**needsRun:** true
**Issue:** For a self-declared Full-tier step, item 4 paraphrases smoke.sh behavior with only the final "PASSED" line, and item 5 says "(Full output in the commit's clean-room run)" - the evidence is asserted, not shown, weakening the log's own "real, pasted output" promise.
**Fix:** Run `bash steps/step-18/smoke.sh` and the clean-room `make verify`, and paste the smoke script's per-check output lines plus the clean-room reactor summary (the 9-module BUILD SUCCESS table) into items 4 and 5.
