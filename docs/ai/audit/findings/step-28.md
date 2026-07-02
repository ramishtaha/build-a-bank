# Step 28 audit - swe:6 pedagogy:4 adhd:3 structure:5 - thinBuild:true

## Strengths

- Exceptional technical honesty: JDK-25 tool compatibility is actually verified, not assumed (PITest 1.25.4 vs 1.19.1 with the real `Unsupported class file major version 69` failure; Error Prone 2.49.0 / NullAway 0.13.6 confirmed working), plus a §12.8 honesty section stating scope limits.
- The Verification Log is genuinely evidential: the §12.3 proof (delete the `verify(publisher).publish(...)` assertion → mutant survives → 80% < 90% threshold → BUILD FAILURE, then revert) demonstrates the gate is real, and the 5-mutant kill table maps each mutator to the test that killed it.
- Strong conceptual framing at the edges: the coverage-vs-mutation misconception is attacked from multiple angles (cheat card, Big Idea, go-deeper, interview Q1, test-yourself ①), with consistent prior-step callbacks (26 hexagon, 27 ArchUnit, 6 auto-config, 1 forward-reference honored).

## Missing spine

- Build sub-step micro-anatomy for ALL 6 sub-steps: no complete code (zero code blocks in the entire Build section), no exact file locations, no line-by-line, no per-sub-step run-and-see, no checkpoints.
- What-we-will-build Mermaid diagram at build open (only an ASCII tree in the B→C bridge, lines 167-180).
- Closing sequence diagram of the flow built (build ends at "The Finished Result" with no diagram).
- Analogy in the Big Idea (diagram present, analogy absent).
- In-lesson flashcards (3-5): recap item (g) at line 424 only points to `docs/flashcards.md`; none are inline.
- Definition-of-Done as a checklist: present only as one inline prose sentence (lines 262-264).
- Session plan with named sittings + per-movement/per-sub-step time-boxes (≈16-hour step, only a whole-step effort figure).
- `requests.http` absent from `steps/step-28/` (`smoke.sh` present; plausibly intentional since no new HTTP endpoints, but note it).

## Findings

### F1: Build section contains no code at all — sacred-build contract violated for every sub-step

**Severity:** high
**Lens:** structure
**Location:** C · Build, Sub-steps 1-6 (lines 191-243)
**needsRun:** true
**Issue:** All six sub-steps are 2-10-line prose descriptions of artifacts ("`NotificationServiceTest`: a new event is applied + published once...", "New module `libs/common`: `MoneyFormatter`... `MoneyProperties`... `MoneyAutoConfiguration`...") with zero code blocks. A nervous beginner at 11pm cannot write `NotificationServiceTest`, the jqwik property, the `-Pmutation` pom profile, the four starter files, `AutoConfiguration.imports`, the `ApplicationContextRunner` test, `HelloMockMvcTesterTest`, `checkstyle.xml`, or the parent-pom gate wiring from these descriptions. The micro-anatomy (Goal → file location → complete code → line-by-line → under-the-hood → run-and-see → checkpoint) is absent in every sub-step. The actual files exist in the repo (e.g. `libs/common/src/main/java/com/buildabank/common/money/`, `services/notification/src/test/java/com/buildabank/notification/domain/`).
**Fix:** For each sub-step, paste the complete file(s) from the repo as fenced code blocks with file-path header comments, package lines, and all imports (pom changes as diff view); add a line-by-line explanation and an under-the-hood note per file; then add a run-and-see with the exact command and freshly captured output (see F3). Source files are already in the working tree at `step-28-end`, so this is transcription plus real runs, not new development.

### F2: No session plan or re-entry support for a ~16-hour step

**Severity:** high
**Lens:** adhd
**Location:** A · Orient "This Step in 30 Seconds" (line 38) and throughout Build
**needsRun:** false
**Issue:** Effort is stated as "≈ 16 hours focused" but there is no breakdown into sittings, no named save points, and no re-entry lines anywhere ("stopping here? you have X working; next session starts at sub-step N, first action: ..."). An ADHD learner has no way to plan the step or to resume after a break; the only save points implied are the three commits.
**Fix:** Add a "Session plan" table to Orient splitting the step into ~6-7 sittings of 2-3h (e.g. S1: Understand + sub-step 1; S2: sub-steps 2-3 mutation capstone; S3: starter classes; S4: starter tests + hello consumption; S5: slice test + gates; S6: Verification Log + clean-room), each ending at a commit or named checkpoint. At the end of each Build sub-step add a one-line re-entry note: what now works, which sub-step is next, and its first concrete action.

### F3: Nothing runs during the build — no first win, no reward loop, no checkpoints

**Severity:** high
**Lens:** adhd
**Location:** C · Build, Sub-steps 1-6 (lines 191-243); first runnable commands appear only in "Play With It" (line 247)
**needsRun:** true
**Issue:** Not a single sub-step ends with a command + expected output. The learner writes tests, a pom profile, a whole new module, and build gates across ~16 hours before anything visibly runs; all evidence is deferred to Movement D. This kills the reward loop, removes every formative checkpoint, and means mistakes in sub-step 1 surface only at the end.
**Fix:** Give every sub-step a run-and-see with the exact command, freshly captured expected output, and a common-wrong-output note, plus a one-line checkpoint. Sub-step 1 must deliver the first win within ~10 minutes: `./mvnw -pl services/notification test -Dtest='NotificationServiceTest,NotificationTest'` showing the green run. Sub-steps: 2 → jqwik `tries = 1000` block; 3 → the PITest score lines; 4 → `-pl libs/common test` then the `hello` test proving injection; 5 → the slice test run; 6 → `spotless:apply` + a planted violation failing `checkstyle:check`. Do not reuse the Movement-D pastes verbatim — capture per-command output.

### F4: Sub-step 4 introduces ~8 new artifacts/annotations in one paragraph

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-step 4 (lines 216-227)
**needsRun:** false
**Issue:** One prose paragraph introduces `MoneyFormatter`, `MoneyProperties`, `@ConfigurationProperties`, `@AutoConfiguration`, `@ConditionalOnProperty(matchIfMissing=true)`, `@ConditionalOnMissingBean`, the `AutoConfiguration.imports` file, and `ApplicationContextRunner` — far beyond the ~3-new-terms-per-sub-step load limit, with no code to anchor any of them. `ApplicationContextRunner` in particular is a brand-new test idiom presented as a name only.
**Fix:** Split into three sub-steps: (4a) the plain classes `MoneyFormatter` + `MoneyProperties` (2 new ideas); (4b) `MoneyAutoConfiguration` + the imports file, explaining each `@ConditionalOn*` as it appears; (4c) `ApplicationContextRunner` tests + consuming from `hello`. Renumber subsequent sub-steps and update the movements table entry for C.

### F5: Build opens without a Mermaid diagram and closes without a sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** "B→C bridge" (lines 167-180) and "The Finished Result" (lines 259-264)
**needsRun:** false
**Issue:** The contract requires a what-we-will-build Mermaid diagram + files tree at build open and a sequence diagram of the built flow at build close. Only an ASCII files tree exists; there is no closing sequence diagram.
**Fix:** Add a Mermaid flowchart before Sub-step 1 showing the four tracks (unit/property tests → PITest gate; libs/common starter → consumed by hello; MockMvcTester slice; Spotless/Checkstyle bound to verify). After the last sub-step add a Mermaid sequenceDiagram of starter discovery (hello starts → Boot reads AutoConfiguration.imports → conditions evaluate → MoneyFormatter bean registered → test injects it) or of the PITest loop (mutate bytecode → re-run tests → killed/survived → threshold check).

### F6: Flashcards not in the lesson

**Severity:** medium
**Lens:** structure
**Location:** Recap item (g) (line 424)
**needsRun:** false
**Issue:** The recap only says flashcards were "appended to `docs/flashcards.md`". The contract requires 3-5 flashcards inline in the recap. Six step-28 cards exist at `docs/flashcards.md` lines 213-219.
**Fix:** Inline 4 of the existing cards (mutation vs coverage; surviving mutant; starter discovery; lean rulesets/Spotless CRLF gotcha) as Q/A bullets under (g), keeping the pointer to `docs/flashcards.md` for the full set.

### F7: DoD requires ADR-0019 that no sub-step creates; sub-steps 1, 2 and 5 have no commit

**Severity:** medium
**Lens:** swe
**Location:** "The Finished Result" (lines 262-264); Sub-steps 1, 2, 5
**needsRun:** false
**Issue:** The Definition of Done demands "ADR-0019 recorded" but no build sub-step instructs writing it (it exists in the repo at `adr/0019-testing-quality-mastery-and-custom-starter.md`, so the learner following the lesson ends non-compliant with their own DoD). Separately, sub-steps 1-2 have no commit of their own (the sub-step-3 commit message happens to cover them) and sub-step 5's work is never explicitly committed — the micro-anatomy requires a commit per sub-step or an explicit statement of which commit carries the work.
**Fix:** Add a short "record the decision" instruction (in sub-step 6 or a closing sub-step): create `adr/0019-testing-quality-mastery-and-custom-starter.md` summarizing the mutation-threshold and lean-gates decisions. State explicitly that the sub-step-3 commit includes sub-steps 1-2, and add sub-step 5's work to the sub-step-6 commit message or give it its own.

### F8: Unpinned jqwik dependency and macOS-only `open` command

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 2 (line 202); "Play With It" (line 250)
**needsRun:** false
**Issue:** Sub-step 2 says "Add the `net.jqwik:jqwik` test dependency" with no version, scope, or pom location — while the lesson elsewhere insists on exact versions (PITest 1.25.4). "Play With It" uses `open services/notification/target/pit-reports/index.html`, which fails on Windows — the primary platform per the cheat card's own `.\mvnw.cmd` note.
**Fix:** In sub-step 2, show the exact `<dependency>` XML (groupId/artifactId/pinned version/`<scope>test</scope>`) and name the file (`services/notification/pom.xml`). In Play With It, replace `open` with a cross-platform line: `start ...` (Windows) / `open ...` (macOS) / `xdg-open ...` (Linux).

### F9: Interactivity collapses to one predict and one break-it; no knowledge checks or type-it-yourself

**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build (lines 191-257)
**needsRun:** false
**Issue:** The toolkit appears only twice (predict in sub-step 1, break-it in sub-step 3). There are no knowledge-checks mid-build, no type-it-yourself, and — since nothing is worked — no scaffold fading toward independence in later sub-steps.
**Fix:** Add a predict-then-run to sub-steps 3 ("which of the 5 mutants would survive if we deleted the duplicate-event test?"), 4 ("the consumer defines its own MoneyFormatter bean — what happens at startup?"), and 6 ("will spotless:check pass before spotless:apply?"). Add one knowledge-check after the starter sub-step (e.g. "which file makes Boot find the auto-config — and what was it called in Boot 2?"). Make sub-step 5 (MockMvcTester) type-it-yourself: give the assertion skeleton and have the learner complete the fluent chain, with the full solution in a collapsed details block.

### F10: Big Idea has no analogy

**Severity:** low
**Lens:** structure
**Location:** B · "The Big Idea" (lines 106-123)
**needsRun:** false
**Issue:** The contract requires big idea + diagram + analogy; the diagram is there, the analogy is not.
**Fix:** Add one line after the first paragraph, e.g.: "Coverage is counting the smoke detectors you installed; mutation testing is lighting a controlled fire and checking an alarm actually rings."

### F11: Stretch exercise points at nonexistent `solutions/step-28/`

**Severity:** low
**Lens:** swe
**Location:** "Your Turn" stretch bullet (line 382)
**needsRun:** false
**Issue:** The stretch challenge promises "reference solution in `solutions/step-28/`", but `solutions/` contains only `step-01`. A learner who attempts the challenge and goes looking hits a dead end.
**Fix:** Reword the bullet to drop the pointer (e.g. "no reference solution yet — verify with your own ApplicationContextRunner web/non-web tests"), or actually add the reference solution folder in a follow-up task.

### F12: No time-boxes below whole-step level; no progress markers; optional content unpriced

**Severity:** low
**Lens:** adhd
**Location:** Movement headers A-F; sub-step headings (lines 191-243); "Go Deeper" (line 362)
**needsRun:** false
**Issue:** Only the whole-step "≈ 16 hours" exists. Movements and sub-steps carry no time estimates, headings don't show "sub-step N of 6" progress, there are no you-are-here markers between movements, and Go Deeper items have no time cost.
**Fix:** Append "· ≈ Xh" to each movement heading and "(≈ Xh)" to each sub-step heading; rename headings to "Sub-step N of 6 — ..."; add a one-line you-are-here marker at the top of movements C and D; label each Go Deeper details block with "(~5 min read)".
