# Step 8 audit - swe:8 pedagogy:8 adhd:5 structure:8 - thinBuild:false

## Strengths

- Engineered-failure pedagogy is exemplary: the missing-table failure (sub-step 9), the non-generic `PostgreSQLContainer` error, and the port-5432 conflict (sub-step 12) are deliberately hit, diagnosed, and fixed in-line, and each one is mirrored in Then-vs-Now, Troubleshooting, and the interview prep - a coherent loop few lessons achieve.
- Code quality against the contract is very high: every file has a file-path header comment, full imports, and package lines; the Boot-4 / Testcontainers-2 module and package deltas (`spring-boot-flyway`, `spring-boot-data-jpa-test`, `@MockitoBean`, non-generic container) are taught explicitly rather than left as magic.
- Full spine compliance: all six movements present, what-we-build roadmap + files tree open the build, a sequence diagram closes it, the Verification Log states its tier (Full) with the hard-to-fake random-port Testcontainers proof and a mutation sanity-check, and exactly 5 flashcards plus 5 test-yourself items target the step's core ideas.

## Missing spine

none

## Findings

### F1: No session plan or sub-step time-boxes for a ~20-hour step

**Severity:** high
**Lens:** adhd
**Location:** A Orient "This Step in 30 Seconds" table (lines 32-43) and C Build opening (lines 248-300)
**needsRun:** false
**Issue:** The step is estimated at ~20 hours but the only time information anywhere is that single whole-step number. There is no planned-sittings breakdown (contract: 6-10 sittings of ~2-3h with named save points), no per-movement time-boxes, and no per-sub-step estimates - an ADHD learner has no way to plan a stopping point before starting.
**Fix:** Insert a "Session plan" table immediately after the build-roadmap Mermaid diagram (after line 267) with ~8 named sittings, e.g.: S1 Orient+Understand (~2.5h, save point: skip-test done); S2 sub-steps 1-3 (~2.5h, save point: V1 migration committed); S3 sub-steps 4-6 (~2.5h); S4 sub-steps 7-8 (~2h); S5 sub-step 9 (~3h, the failure-and-fix); S6 sub-steps 10-11 (~2.5h); S7 sub-step 12 + Play With It (~2.5h); S8 Prove+Apply+Review (~2.5h). Also append an "(~Xh)" estimate to each sub-step heading (e.g., "Sub-step 9 of 12 ... (~2.5h)").

### F2: No re-entry support at checkpoints

**Severity:** medium
**Lens:** adhd
**Location:** Checkpoint blocks of sub-steps 3 (line 707), 5 (line 841), 8 (line 1107), 9 (line 1273), 11 (line 1502)
**needsRun:** false
**Issue:** Every sub-step has a checkpoint, but none tells a learner who stops there what they have working and what the first action of the next session is - the contract's "stopping here? you have X working; next session starts at sub-step N, first action: ..." line is absent throughout.
**Fix:** Append a re-entry line to the checkpoints at the natural break points listed above, e.g. after sub-step 8: "Stopping here? You have the complete main source set (entity -> controller -> config) compiling; nothing runs yet by design. Next session starts at sub-step 9; first action: `docker info` to confirm Docker is up, then create `ContainersConfig.java`."

### F3: Sub-step 1 Run & See asserts an unverified (and likely wrong) failure for `verify`

**Severity:** high
**Lens:** swe
**Location:** Sub-step 1, Predict + Run & See (lines 464-485)
**needsRun:** true
**Issue:** The predict asks what `./mvnw -pl services/cif -am verify` does, and the text answers "this will actually *fail to start*" because "the context starts but ... there's no datasource configured." At this point the module has zero tests, so nothing ever boots the Spring context during `verify` - the build almost certainly succeeds. No actual `verify` output is shown; the only pasted output is for the fallback `compile` command. The lesson's own "verify, don't guess" rule is violated by its first run-and-see.
**Fix:** Check out the state at end of sub-step 1 (pom + `CifApplication` only), actually run `./mvnw -pl services/cif -am verify`, paste the real (abridged) output, and rewrite the predict answer to match reality (likely: "it succeeds - with no tests, nothing starts the context; the first honest failure arrives when tests exist"). Keep the compile command as the checkpoint command.

### F4: Run & See (command + expected output + common-wrong-output) missing for sub-steps 2-6 and 8

**Severity:** medium
**Lens:** structure
**Location:** Sub-steps 2 (lines 498-662), 3 (665-717), 4 (720-769), 5 (772-851), 6 (854-951), 8 (1047-1117)
**needsRun:** true
**Issue:** Half of the twelve sub-steps have no formal Run & See block: the micro-anatomy jumps from predict (or under-the-hood) straight to checkpoint. Some checkpoints mention a compile command inline, but there is no expected-output block and no common-wrong-output for six consecutive sub-steps - the contract requires the full run-and-see triple for every sub-step.
**Fix:** Add a "Run & See" block to each of these sub-steps using `./mvnw -pl services/cif -DskipTests compile` (and for sub-step 3, an `ls`/`dir` of `src/main/resources/db/migration` to confirm the exact filename), run each command once to capture the real output, and add one common-wrong-output apiece (e.g., sub-step 2: a missing `jakarta.persistence` import compile error; sub-step 3: the single-underscore filename shown by the listing).

### F5: Seven-sub-step reward gap - nothing visibly runs between sub-step 1 and sub-step 9

**Severity:** medium
**Lens:** adhd
**Location:** Sub-steps 2-8 (lines 498-1117); compose.yaml deferred to sub-step 12 (line 1519)
**needsRun:** true
**Issue:** From the entity (sub-step 2) through application.yml (sub-step 8) - plausibly 6+ hours of work - the only feedback is `BUILD SUCCESS` from compiles. The first time the learner sees the app *do* anything (Flyway lines, a green test) is sub-step 9. The compose file that would let the app boot live already has everything it needs after sub-step 8, but it is withheld until sub-step 12.
**Fix:** Move the creation of `services/cif/compose.yaml` from sub-step 12 into a short optional "boot it now (~10 min)" insert at the end of sub-step 8: `docker compose -f services/cif/compose.yaml up -d` + `./mvnw -pl services/cif spring-boot:run`, run it once and paste the real Flyway-migrate + "Tomcat started on port 8081" lines. Sub-step 12 then keeps the HTTP exercising, the port-5432 conflict story, and the teardown.

### F6: "Silently swaps in H2" claim is impossible with this pom

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 9 pitfall (line 1282) and Troubleshooting "runs against H2, not Postgres" entry (line 1904)
**needsRun:** true
**Issue:** The pitfall claims that omitting `@AutoConfigureTestDatabase(replace = NONE)` means `@DataJpaTest` "silently swaps in H2, and your 'real Postgres' test isn't testing Postgres at all." The module's pom declares no H2 (or any embedded DB) dependency, so a swap-in cannot happen - the true behavior is either a "Failed to replace DataSource ... no embedded database on the classpath" failure, or (since Boot 3.4 the test-database support backs off for `@ServiceConnection`-backed DataSources) the test passing unchanged, which would make the annotation redundant rather than protective. Either way the stated failure mode is wrong.
**Fix:** Delete the `@AutoConfigureTestDatabase(replace = NONE)` line, run `./mvnw -pl services/cif -am test -Dtest=CustomerRepositoryTest`, and rewrite the pitfall and the troubleshooting entry to describe the actually observed behavior (keeping or dropping the annotation in the taught code accordingly, with the honest rationale).

### F7: Sub-step 9 overloads working memory (~7 new annotations plus a break-it in one sub-step)

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-step 9 (lines 1120-1283)
**needsRun:** false
**Issue:** One sub-step introduces `@TestConfiguration(proxyBeanMethods=false)`, `@ServiceConnection`, the non-generic `PostgreSQLContainer`, `@DataJpaTest` (new package), `@Import`, `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`, and `@AutoConfigureTestDatabase(replace=NONE)` - roughly seven new tokens - plus a break-it-on-purpose failure cycle. This is more than double the ~3-new-terms budget and is the step's cognitive peak with no split.
**Fix:** Split into sub-step 9a (ContainersConfig only: `@TestConfiguration`, `@ServiceConnection`, non-generic container - ending with a checkpoint that it compiles) and 9b (the slice test: `@DataJpaTest`, `@Import`, the Flyway import, `replace=NONE`, then the engineered missing-table failure and fix). Renumber to 13 sub-steps and update the roadmap diagram and "X of N" markers.

### F8: No scaffold fading - all twelve sub-steps stay fully worked, with a single knowledge-check

**Severity:** medium
**Lens:** pedagogy
**Location:** Whole build (lines 248-1668); only knowledge-check at line 757
**needsRun:** false
**Issue:** Every sub-step is copy-paste-complete from first to last; nothing shifts toward type-it-yourself even where the pattern has already been fully worked twice (the second GET endpoint, the third MockMvc test, the second record DTO). Interactivity is predict-heavy: one knowledge-check in the whole build, zero type-it-yourself prompts (the IntelliJ generate tip is the closest thing).
**Fix:** Convert three late repeats into type-it-yourself with a hidden `<details>` solution: the `byNumber` endpoint in sub-step 7 ("you wrote `byId`; now write `byNumber` yourself - same Optional chain"), the `missingCustomerReturns404` test in sub-step 10, and the GET-half of the integration test in sub-step 11. Add knowledge-checks after sub-steps 5 (why `readOnly=true`?) and 8 (what does `${VAR:default}` resolve first?).

### F9: Interview Q5 wrongly claims an absent date of birth triggers a 400

**Severity:** low
**Lens:** swe
**Location:** E Apply, Interview Q5 (line 1845)
**needsRun:** false
**Issue:** The answer lists "future/absent DOB" among Bean Validation 400 causes. `dateOfBirth` carries only `@Past`, which (like all Bean Validation constraints except `@NotNull`/`@NotBlank`/`@NotEmpty`) treats `null` as valid, and the column is nullable - so an absent DOB passes validation and creates the customer. The lesson elsewhere never claims DOB is mandatory.
**Fix:** In Q5's answer change "future/absent DOB" to "future DOB", and optionally add one sentence: "note a *missing* DOB passes - `@Past` ignores `null`; you'd add `@NotNull` to make it mandatory."

### F10: Port-5432 conflict story skips the more likely first symptom (compose bind failure)

**Severity:** low
**Lens:** swe
**Location:** Sub-step 12, "The port-5432 conflict" (lines 1566-1588) and Troubleshooting failure 3 (lines 1892-1898)
**needsRun:** false
**Issue:** The narrative jumps straight to the app's `FATAL: password authentication failed` - but if something already listens on host 5432, `docker compose up -d` itself typically fails to bind the port, so the learner's *first* error is a compose port-allocation failure, and the auth failure only appears in the specific case where the container never started (or a native Postgres answers) and the app is run anyway. A learner seeing the compose error first won't match it to this section.
**Fix:** Add two prose sentences before the FAILURE block: "Depending on your setup you may hit this earlier: `docker compose up` itself can fail because host port 5432 is already allocated - check `docker compose ps`. If the container silently failed to start and a different Postgres answered on 5432, the app instead fails at connect time with:" (do not fabricate the compose error output; the existing auth-failure block stays as the captured example).

### F11: Optional content carries no time cost

**Severity:** low
**Lens:** adhd
**Location:** E Apply "Go Deeper" (lines 1790-1814), IntelliJ aside (line 661), Play With It experiments (lines 1684-1689)
**needsRun:** false
**Issue:** The four Go-Deeper collapsibles, the IntelliJ tip, and the four "little experiments" are all optional but unlabeled for time - an ADHD learner cannot tell a 3-minute read from a 30-minute rabbit hole, which invites either skipping everything or losing an hour.
**Fix:** Add a time label to each optional item's summary/heading, e.g. "Go Deeper (optional, ~5 min read each)", "Faster in IntelliJ (optional, ~2 min)", "Little experiments (~5 min each; revert after each)".

### F12: Live-run "pasted output" in the Verification Log is stylized, not raw

**Severity:** medium
**Lens:** swe
**Location:** D Prove section 2 (lines 1731-1742) and sub-step 12 expected outputs (lines 1606-1626)
**needsRun:** true
**Issue:** The live-run evidence is summarized ("POST /api/customers -> HTTP 201 Location: /api/customers/1", "GET ... -> HTTP 200 (same JSON)") rather than the real `curl -i` transcript (status line, headers). Only the JSON body line reads like genuine capture. For a Full-tier Verification Log that leans on "hard to fake" proof, paraphrased HTTP lines undercut the honesty standard the lesson itself sets in the sandbox-honesty note.
**Fix:** Re-run the four curl commands against the live service and replace the stylized lines in both locations with the raw response head (e.g., `HTTP/1.1 201`, the actual `Location:` and `Content-Type:` headers) plus the body, trimming uninteresting headers with an explicit `...` marker.
