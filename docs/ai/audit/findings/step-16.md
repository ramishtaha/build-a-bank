# Step 16 audit - swe:7 pedagogy:6 adhd:4 structure:8 - thinBuild:true

## Strengths

- The Verification Log (D) is exemplary for the fleet: stated 🔴 Full tier, real pasted JWT with decoded payload, the §12.3 mutation with the actual failing assertion output, BCrypt evidence, smoke.sh output, and a clean-room clone check.
- Conceptual teaching in B is accurate, modern, and interview-aligned: filter-chain mechanics (FilterChainProxy, BearerTokenAuthenticationFilter, AuthorizationFilter), 401-vs-403, JWT structure, HMAC-vs-asymmetric trade-offs, CSRF rationale, and the observed Spring Security 7 FACTOR_BEARER behavior called out as "verified, don't guess".
- Strong review/retrieval apparatus: you-are-here markers on every sub-step, a symptom→cause→fix troubleshooting table, 5 on-target test-yourself items, 5 flashcards, and a résumé line.

## Missing spine

- Complete code that "compiles as shown" with file-path header comments: absent in ALL 5 build sub-steps (every block is a fragment: "full file in the repo", "the essence").
- Run-and-see (exact command + expected output + common-wrong-output): absent in sub-steps 0, 1, 2; common-wrong-output absent everywhere.
- Under-the-hood: absent in sub-steps 0 and 3.
- Predict-then-run: present only in sub-step 1 (absent in 0, 2, 3, 4).
- Type-it-yourself: absent from the entire build (no scaffold fading).
- Several files promised in the files-we'll-touch tree are never built in any sub-step: `application.yml`, `AuthApplication.java`, `web/AuthDtos.java`, `steps/step-16/requests.http`, `steps/step-16/smoke.sh`, `adr/0008-...md`.
- Session plan / per-sub-step time-boxes / re-entry lines for a ~20-hour step: absent.

## Findings

### F1: Build code is fragments — a beginner cannot produce the module from the lesson

**Severity:** high
**Lens:** swe
**Location:** C Build, sub-steps 0-4 (lines ~255-441)
**needsRun:** false
**Issue:** Every code block is an excerpt ("full file in the repo", "the essence"). `SecurityConfig` is shown without package line, imports, class declaration, fields, the `secretKey()` method it calls, or the `jwtAuthConverter()` bean it references; `UserService`/`JwtService` lack class skeletons, the `users` map, the `StoredUser` record, the seeding code, and `ttlSeconds()`; `AuthDtos.java` (LoginRequest/TokenResponse/MeResponse), `AuthApplication.java`, and `application.yml` (port 8083, HMAC secret, issuer, TTL) are never shown anywhere. The sacred-build promise — "a nervous beginner at 11pm completes it with no outside help" — fails at sub-step 0.
**Fix:** Replace each fragment with the complete file from the repo at `step-16-end`, each with a `// services/auth/src/main/java/...` first-line path comment, package line, and all imports. Add the missing files to their owning sub-steps: `AuthApplication.java` + `application.yml` + full `services/auth/pom.xml` to sub-step 0; `secretKey()` + `jwtAuthConverter()` to sub-step 1; `StoredUser` + seeding + `ttlSeconds()` to sub-step 2; `AuthDtos.java` to sub-step 3; full test classes incl. the `login()`/`get()`/`tokenFor()` helpers to sub-step 4.

### F2: No session plan for a ~20-hour step

**Severity:** high
**Lens:** adhd
**Location:** A Orient (after "Before You Start", line ~114)
**needsRun:** false
**Issue:** Effort is stated as ≈20 hours but there is no sitting-by-sitting plan, no named save points, and no per-movement time budget. An ADHD learner has no way to slice this into sessions or know where to safely stop.
**Fix:** Insert a "🗓️ Session plan" table after Before You Start with 7-8 sittings of 2-3h, e.g.: S1 Orient+Big Idea; S2 rest of Understand; S3 sub-steps 0-1 (save point: SecurityConfig commit); S4 sub-step 2 (save point: user store commit); S5 sub-step 3 + Play With It; S6 sub-step 4 + mutation; S7 Verification Log; S8 Apply+Review. Each row: goal, save-point commit message, and "next session starts at ..." first action.

### F3: Promised artifacts (requests.http, smoke.sh, ADR-0008) have no build sub-step

**Severity:** high
**Lens:** structure
**Location:** C Build, files-we'll-touch tree (lines ~237-244) vs sub-steps 0-4
**needsRun:** false
**Issue:** The tree promises `steps/step-16/{requests.http,smoke.sh}` and `adr/0008-...md`, and the DoD requires `bash steps/step-16/smoke.sh` to pass — but no sub-step creates any of them. The learner following the build top-to-bottom cannot satisfy the DoD checklist or the tree.
**Fix:** Add "Sub-step 5 — Prove-it artifacts" with the full contents of `requests.http`, `smoke.sh`, and ADR-0008 (copied from the repo), micro-anatomy (goal/location/line-by-line/checkpoint/commit/pitfall), and a run-and-see for `bash steps/step-16/smoke.sh` reusing the already-captured output from Verification Log §5. Update the sub-step flowchart and you-are-here markers.

### F4: No visible-output run-and-see until sub-step 3 — reward loop too sparse

**Severity:** high
**Lens:** adhd
**Location:** C Build, sub-steps 0, 1, 2 (lines ~249-359)
**needsRun:** true
**Issue:** Sub-steps 0-2 (roughly the first half of the build, hours of work) end with checkpoints like "compiles" or a bare `dependency:resolve` with no expected output. Nothing visibly runs in the first sitting of the build; the first on-screen result is at sub-step 3. That kills the run-something-see-something loop the rubric requires.
**Fix:** Add a run-and-see with real pasted output to each: sub-step 0 — `./mvnw -q -pl services/auth dependency:resolve` (paste actual tail) plus start the empty app and show Boot's default all-locked-down 401 (a first win that also demonstrates the sub-step-0 pitfall); sub-step 1 — restart and curl `/actuator/health` (200, now permitAll) vs `/api/auth/me` (401); sub-step 2 — a 2-line JShell or temporary test printing a `$2a$10$...` BCrypt hash twice to show differing salts. Run the commands and paste truthful output; do not invent it.

### F5: Sub-step 1 introduces ~10 new API tokens in one block; `.cors(...)` never explained

**Severity:** medium
**Lens:** pedagogy
**Location:** C Build, sub-step 1 (lines ~278-321)
**needsRun:** false
**Issue:** One code block introduces `csrf.disable()`, `SessionCreationPolicy.STATELESS`, `cors(Customizer.withDefaults())`, `authorizeHttpRequests`, `requestMatchers`, `hasRole`, `oauth2ResourceServer`, `jwtAuthenticationConverter`, `PasswordEncoder`, `NimbusJwtEncoder`, `ImmutableSecret`, `NimbusJwtDecoder`, and `HS256` — far beyond ~3 new terms per sub-step. Worse, the `cors` line gets no line-by-line bullet at all, violating "every new token explained on first appearance".
**Fix:** Split into sub-step 1a (access rules: csrf/session/authorizeHttpRequests, with its own checkpoint+commit) and 1b (machinery: JwtEncoder/JwtDecoder/BCrypt beans + converter). Add a line-by-line bullet for `cors(Customizer.withDefaults())` (what CORS is in one sentence, that it delegates to a `CorsConfigurationSource`, and why a token API still needs it for browser clients — or delete the line if the repo config doesn't define a source).

### F6: CORS is in the objectives and TOC but never taught

**Severity:** medium
**Lens:** pedagogy
**Location:** A Orient outcomes (line ~99) + B TOC row (line ~17) vs B Understand body
**needsRun:** false
**Issue:** The outcome "Reason about CSRF/CORS/headers" and the movement-B TOC entry promise CORS, but the Understand movement explains only CSRF and headers — CORS is never defined, has no under-the-hood paragraph, no test-yourself item, and no checkpoint. The objective is unassessed and unaligned.
**Fix:** Either (a) add a short "CORS in one minute" paragraph to Under the Hood (same-origin policy, preflight, why the filter chain must answer OPTIONS before auth) plus one test-yourself question, or (b) remove "CORS" from the outcome bullet, the 30-seconds row, and the TOC row and defer it explicitly to the step that configures it (e.g. "CORS: Step 29's gateway config").

### F7: No scaffold fading — zero type-it-yourself, one predict in the whole build

**Severity:** medium
**Lens:** pedagogy
**Location:** C Build, sub-steps 2-4
**needsRun:** false
**Issue:** All sub-steps are fully worked (to the extent code is shown); the interactivity toolkit is nearly absent from the build: one predict-then-run (sub-step 1) and one break-it (sub-step 4). Later sub-steps should shift effort to the learner.
**Fix:** Convert the `/admin` endpoint in sub-step 3 and the last two assertions in sub-step 4 into type-it-yourself tasks (state the spec — "write the test that logs in as admin and expects 200 on /admin" — with the solution in a collapsed `<details>`). Add predicts to sub-step 2 ("encode('password') twice — same output? why not?") and sub-step 4 ("before running: which test fails if you delete the permitAll for /login?").

### F8: No re-entry support or per-sub-step time-boxes

**Severity:** medium
**Lens:** adhd
**Location:** C Build, all checkpoints (sub-steps 0-4)
**needsRun:** false
**Issue:** Checkpoints confirm state but never orient a returning learner; there are no time estimates on movements or sub-steps (only the 20h whole-step figure), and orient's "§12.3 mutation" / "§12.4 clean-room" jargon is used without a one-line micro-recap of what those conventions are.
**Fix:** Append to each sub-step checkpoint a re-entry line: "🛑 Stopping here? You have <X> working and committed; next session starts at Sub-step N — first action: open <file>." Add "≈ X h" to each sub-step heading and each movement heading. At first use of "§12.3 mutation" add "(the course convention from Step 12: deliberately break the rule under test, watch the test fail, revert)".

### F9: "Under the Hood" is a wall of text

**Severity:** medium
**Lens:** adhd
**Location:** B Understand, "🌱 Under the Hood" (lines ~169-185)
**needsRun:** false
**Issue:** Eight consecutive dense paragraphs (~700 words) covering filter internals, DSL, Nimbus, claim mapping, BCrypt, CSRF, headers, and SS7 factors with no diagram, code block, or interactive break — the worst wall-of-text in the lesson.
**Fix:** Break it up: insert a small Mermaid diagram of the filter order (FilterChainProxy → BearerTokenAuthenticationFilter → AuthorizationFilter → 401/403 handlers) after the first paragraph; convert the history sentence into the existing Then-vs-Now table (delete the duplication); put BCrypt's `$2a$10$...` anatomy in a short annotated code block; add H4 sub-headings so each topic is a scannable chunk.

### F10: `make run-auth` is instructed but never created or verified in the lesson

**Severity:** medium
**Lens:** swe
**Location:** A Orient 30-seconds table (line ~36) and C "Play With It" step 1 (line ~466)
**needsRun:** true
**Issue:** The lesson tells the learner to run `make run-auth`, but the Makefile is not in the files-we'll-touch tree, no sub-step adds the target, and no pasted output proves it works. If the target doesn't exist at `step-16-end`, the first Play-With-It instruction fails.
**Fix:** Check out `step-16-end` and run `make run-auth`. If the target exists, add the Makefile line to the files-we'll-touch tree and to sub-step 0's code (diff view) with a one-line explanation; if it doesn't, replace both mentions with `./mvnw -pl services/auth spring-boot:run` (already shown in the cheat card).

### F11: Sub-step 4 pitfall misattributes the real filter chain to spring-security-test; "32-char" secret conflates chars and bytes

**Severity:** low
**Lens:** swe
**Location:** Sub-step 4 pitfall (line ~441) and troubleshooting row 2 (line ~596)
**needsRun:** false
**Issue:** With `@SpringBootTest(webEnvironment = RANDOM_PORT)` over real HTTP, the real filter chain applies because a real server is running — `spring-security-test` (MockMvc post-processors, `@WithMockUser`) plays no role in that. Separately, the troubleshooting fix "use a ≥ 256-bit (≥ 32-char) secret" is only true for single-byte characters; HS256 requires ≥ 32 bytes.
**Fix:** Reword the pitfall to: "RANDOM_PORT tests hit a real server, so the real filter chain applies — unauthenticated requests really 401; `spring-security-test` is only needed for MockMvc-style tests (`@WithMockUser`), not this class." Change the troubleshooting fix to "use a secret of ≥ 32 bytes (32+ ASCII characters)."

### F12: Sub-step numbering "0 of 4" ... "4 of 4" labels 5 sub-steps as 4

**Severity:** low
**Lens:** structure
**Location:** C Build, all sub-step headings (lines ~249, 278, 324, 362, 409)
**needsRun:** false
**Issue:** There are five sub-steps labeled "Sub-step 0 of 4" through "Sub-step 4 of 4" — the zero-indexing makes "of 4" undercount and undermines the visible-progress marker ("4 of 4" reads as done when a fifth exists once F3's artifact sub-step is added).
**Fix:** Renumber to "Sub-step 1 of 5" ... "Sub-step 5 of 5" (or "of 6" after adding F3's artifacts sub-step), and update the build flowchart node labels, the you-are-here markers, and any cross-references (e.g. "the next sub-step's config" in sub-step 0's pitfall).
