# Step 27 audit - swe:7 pedagogy:5 adhd:4 structure:6 - thinBuild:true

## Strengths

- Verification Log is exemplary: real pasted output for both green suites AND both §12.3 mutations (ArchUnit `@Component`-on-domain violation, Modulith `event→outbox` cycle), plus an honest §12.8 caveat block. Spot-checked against the repo: the four rule names, `archunit.version=1.4.2`, `spring-modulith.version=2.0.6` + BOM import, `adr/0018-archunit-and-spring-modulith.md`, and the step-27 flashcards in `docs/flashcards.md` all exist exactly as claimed.
- The step's central gotcha (ArchUnit reads bytecode, so an unused import is invisible) is encoded five ways: pattern spotlight, predict-then-run, troubleshooting entry, interview Q2, and a flashcard — genuinely deep misconception work.
- The which-tool-when contrast (ArchUnit = bespoke authored rules vs Modulith = derived modules + universal rules + docs) is carried consistently from cheat card through the B→C bridge to interview prep, go-deeper, and recap.

## Missing spine

- Build-opening what-we-will-build Mermaid diagram + files-we-will-touch tree
- Build-closing sequence diagram of the flow built
- Per-sub-step micro-anatomy for sub-steps 1-3: no complete code, no line-by-line, no run-and-see, no checkpoint, no per-sub-step commit (only one commit at sub-step 4)
- Analogy in B · Understand (big idea + diagram present, analogy absent)
- Flashcards not inline in the recap — (g) is only a pointer to `docs/flashcards.md` (6 cards exist there)
- Definition-of-Done rendered as prose, not a checklist
- Session plan / per-movement and per-sub-step time-boxes / re-entry lines (ADHD contract)
- `requests.http` absent from `steps/step-27/` (`smoke.sh` present; arguably N/A — this step adds no HTTP surface, and neighboring steps 26/28 also omit it)

## Findings

### F1: Build contains no code — sub-steps 1-3 are goal-only stubs
**Severity:** high
**Lens:** swe
**Location:** C · Build, Sub-steps 1-3 (lines 212-234)
**needsRun:** false
**Issue:** The sacred build never shows a single complete file. Sub-step 1 describes three pom.xml edits in prose with no XML/diff. Sub-step 2 names four ArchUnit rules but only two ever appear anywhere (as fragments in B · Understand; the `layeredArchitecture` snippet is an unassigned expression that doesn't compile standing alone) — `application_does_not_depend_on_adapters` and `application_is_transport_agnostic` are never shown at all. Sub-step 3's `ModularityTest` has zero code. A nervous beginner at 11pm cannot produce these files from the lesson.
**Fix:** Paste the real, already-existing artifacts with file-path header comments, all imports, and line-by-line explanations: (1) diff-view blocks for the parent pom (`spring-modulith.version` property + `spring-modulith-bom` import), `services/notification/pom.xml` (`archunit-junit5`, test scope) and `services/demand-account/pom.xml` (`spring-modulith-starter-test` + `spring-modulith-docs`, test scope); (2) the full `services/notification/src/test/java/com/buildabank/notification/HexagonalArchitectureTest.java` (all four `@ArchTest` rules); (3) the full `services/demand-account/src/test/java/com/buildabank/account/ModularityTest.java`. No output needs inventing — the code exists in the repo at `step-27-end`.

### F2: No per-sub-step run-and-see or checkpoints; first executable command comes after all four sub-steps
**Severity:** high
**Lens:** pedagogy
**Location:** C · Build, Sub-steps 1-4 (lines 212-241) and 🎮 Play With It (line 243)
**needsRun:** true
**Issue:** The only runnable commands in the build live in Play With It, after the whole build is done. No sub-step has a run-and-see (exact command + expected output + common-wrong-output) or a checkpoint, so the learner gets zero formative feedback and no first win within the first 10 minutes (sub-step 1 is pom wiring with nothing to run).
**Fix:** Capture real output and add per-sub-step run-and-see: after sub-step 1, a dependency verification (e.g. `./mvnw -pl services/notification dependency:tree | grep archunit` and the Modulith equivalent) with pasted output; after sub-step 2, `./mvnw -pl services/notification -Dtest=HexagonalArchitectureTest test` with the real 4/4 output plus common-wrong-outputs ("No tests were executed" from a mistyped `-Dtest`; a vacuous pass from a wrong `@AnalyzeClasses` package); after sub-step 3, the ModularityTest run + `ls target/spring-modulith-docs/`. End each sub-step with a ✅ checkpoint line stating what now works.

### F3: No session plan, no time-boxes below whole-step level, no re-entry support
**Severity:** high
**Lens:** adhd
**Location:** A · Orient "This Step in 30 Seconds" (line 37) and C · Build headings
**needsRun:** false
**Issue:** The step claims ≈8 hours of focused work but plans zero sittings: no save points, no per-movement or per-sub-step time estimates, no "stopping here?" re-entry lines. An ADHD learner has no way to slice this or resume mid-build.
**Fix:** Add a "Session plan" box in Orient: S1 (~2h) Understand + sub-step 1 (save point: deps wired, dependency:tree clean); S2 (~3h) sub-steps 2-3 (save point: both suites green); S3 (~2.5h) sub-step 4 mutation + Prove + Apply/Review (save point: `step-27-end` tagged). Put a time estimate on each sub-step heading and a re-entry line at each checkpoint, e.g. "Stopping here? Both deps are wired and the build is green. Next session starts at Sub-step 2 — first action: create `HexagonalArchitectureTest.java` under `services/notification/src/test/java/com/buildabank/notification/`."

### F4: Build missing its opening diagram + files-touched tree and closing sequence diagram
**Severity:** high
**Lens:** structure
**Location:** C · Build, "📦 Your Starting Point" (line 207) and end of Build (line 259)
**needsRun:** false
**Issue:** The contract requires the build to open with a what-we-will-build Mermaid diagram + files-we-will-touch tree and close with a sequence diagram of the flow built. All three are absent (the only diagram is the module DAG up in Understand).
**Fix:** After "Your Starting Point", add a Mermaid flowchart (notification hexagon guarded by HexagonalArchitectureTest; demand-account's 9 packages guarded by ModularityTest; both feeding `./mvnw verify` → red on violation) plus a file tree listing the three pom.xml edits, the two test classes, and `adr/0018-archunit-and-spring-modulith.md`. After Play With It, add a Mermaid sequenceDiagram: developer adds `@Component` to domain → `mvnw test` → surefire → ArchUnit imports bytecode → rule evaluates → violation report → BUILD FAILURE → revert → green.

### F5: ADR-0018 is required by the Definition of Done but never authored in the build
**Severity:** medium
**Lens:** swe
**Location:** C · "🏁 The Finished Result" (lines 257-259); sub-steps 1 and 3 cite "ADR-0018 §3"
**needsRun:** false
**Issue:** DoD says "ADR-0018 recorded" and two sub-steps cite its §3, but no sub-step tells the learner to write it. The file exists in the repo (`adr/0018-archunit-and-spring-modulith.md`) — the lesson just never builds it, so a learner following top-to-bottom fails their own DoD.
**Fix:** Add a short sub-step (between 3 and 4, or folded into 4 before the commit): "Record ADR-0018" with the exact path `adr/0018-archunit-and-spring-modulith.md` and the file's content (or a summarized template covering: decision = test-scoped Modulith + bespoke ArchUnit for the hexagon; §3 = why test scope only).

### F6: Flashcards are a pointer, not present in the lesson
**Severity:** medium
**Lens:** structure
**Location:** F · Recap (g) (line 395)
**needsRun:** false
**Issue:** Recap item (g) only says flashcards are "appended to `docs/flashcards.md`". The contract requires 3-5 flashcards in the recap. (Six step-27 cards do exist at `docs/flashcards.md` line 205ff.)
**Fix:** Inline 3-5 of the existing step-27 Q/A cards under (g) (fitness function definition; bytecode vs source; how Modulith defines a module + what `verify()` checks; ArchUnit vs Modulith; how you proved the guards), keeping the pointer line for the full set.

### F7: Understand movement has no analogy
**Severity:** medium
**Lens:** pedagogy
**Location:** B · "🧠 The Big Idea" (lines 105-119)
**needsRun:** false
**Issue:** Contract requires big idea + diagram + analogy; only the first two are present. For an abstract topic (fitness functions over bytecode) a concrete anchor is exactly what a beginner needs.
**Fix:** Add a 2-3 sentence analogy after the big idea, e.g.: a wiki architecture diagram is a blueprint pinned to the wall — nothing stops the builders deviating. A fitness function is the building inspector who examines the actual structure (the compiled bytecode) on every build and refuses the occupancy permit (fails the build) when a wall is where a door should be. ArchUnit is the inspector you hand a custom checklist; Modulith is the standard code book applied to every room.

### F8: No you-are-here or sub-step progress markers
**Severity:** medium
**Lens:** adhd
**Location:** C · Build sub-step headings (lines 212, 219, 225, 235)
**needsRun:** false
**Issue:** Sub-steps are numbered but carry no "X of N", no time estimate, and there are no you-are-here markers anywhere in the build, so mid-build progress is invisible — a known drop-out driver.
**Fix:** Add a marker line under each sub-step heading: "📍 You are here: Build sub-step 2 of 4 · ~90 min · notification's hexagon rules" (and equivalents for 1, 3, 4), plus a one-line movement-level marker at the top of C ("Movement C of 6 · the longest one").

### F9: Interactivity toolkit sparse — one predict, no type-it-yourself, no knowledge checks in the build
**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, Sub-steps 1-3 (lines 212-234)
**needsRun:** false
**Issue:** The entire build has a single predict-then-run (sub-step 2). No type-it-yourself (so no scaffold fading — later sub-steps are exactly as unworked as early ones), and no knowledge checks. Sub-step 4 is a good break-it-on-purpose, but it's the only interaction.
**Fix:** (1) After sub-step 1, add a knowledge check: "What does importing `spring-modulith-bom` add to your classpath? — Nothing; a BOM only pins versions in `dependencyManagement`; the starter/docs deps add the jars." (2) In sub-step 2, once the first three rules are given as worked examples (per F1), make the fourth rule (`application_is_transport_agnostic`) type-it-yourself from a spec ("write a `noClasses()` rule forbidding `..application..` from depending on web/servlet/Kafka packages") with the solution in a `<details>`. (3) Before sub-step 3's first run, add a predict: "9 modules — will `verify()` pass? Which single edge in the DAG diagram would create a cycle?"

### F10: Definition of Done is prose, not a checklist
**Severity:** low
**Lens:** structure
**Location:** C · "🏁 The Finished Result" (lines 257-259)
**needsRun:** false
**Issue:** The DoD is one bold run-on sentence; the contract specifies a checklist, and a scannable checklist is also the ADHD-friendly form.
**Fix:** Convert to checkboxes: `- [ ] HexagonalArchitectureTest 4/4 green (no Docker)` / `- [ ] ModularityTest 3/3 green, docs in target/spring-modulith-docs/` / `- [ ] §12.3 mutations went red, reverted to green` / `- [ ] ./mvnw verify green (Docker up)` / `- [ ] bash steps/step-27/smoke.sh passes` / `- [ ] ADR-0018 recorded` / `- [ ] committed + tagged step-27-end`.
