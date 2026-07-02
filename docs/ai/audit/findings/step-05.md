# Step 5 audit - swe:8 pedagogy:8 adhd:6 structure:8 - thinBuild:false

## Strengths

- Complete, compile-plausible code in every sub-step (file-path headers, full imports, package lines) with arithmetically verified outputs (SpEL 3.25%, interest 325.00/475.00, banker's rounding) and a real Verification Log whose lifecycle ordering (1-4, @PreDestroy last) is hard to fake.
- Dense interactivity: predict-then-run with hidden answers in nearly every sub-step, three break-it-on-purpose experiments with realistic error text, a change-X-see-Y play table, and you-are-here markers on all 11 sub-steps.
- All six movements present with resolving anchors; misconceptions squarely addressed (field injection, @Bean methods are proxied not plain calls, prototype lifecycle not managed, jakarta vs javax).

## Missing spine

- requests.http absent from steps/step-05/ (explicitly waived in-lesson: non-web step, "nothing to curl yet" - acceptable but noting per folder contract)
- Run-and-see (exact command + expected output + common-wrong-output) absent from build sub-steps 1 and 3-8; checkpoints say "compiles" with no command shown except sub-step 2
- Pitfall element absent from sub-steps 3 and 8
- ADHD session architecture absent: no planned sittings, no per-movement/per-sub-step time-boxes, no re-entry lines at checkpoints (only the whole-step "≈20h")

## Findings

### F1: Sub-step 9 Run & See is guaranteed to fail as sequenced

**Severity:** high
**Lens:** swe
**Location:** Sub-step 9 of 11 (LabRunner), "Run & See" through the closing Pitfall (lines ~969-1005)
**needsRun:** false
**Issue:** The Run & See tells the learner to run `./mvnw -pl playground/spring-lab spring-boot:run` "now that the app is functional end-to-end" and shows successful expected output - but `application.yml` is not created until sub-step 10, and `@Value("${bank.name}")` has no default, so this run actually fails with `Could not resolve placeholder 'bank.name'`. The pitfall at the very end of the sub-step admits this, but it comes after the expected-success output, the checkpoint ("you see all four lifecycle lines...") and the commit - a nervous beginner will run it, see a stack trace that contradicts the printed expected output, and stall.
**Fix:** Reorder: make sub-step 9 = application.yml (current sub-step 10 content, minus the jar run) and sub-step 10 = LabRunner, moving the existing Run & See block, expected output, both common-wrong-outputs, and the checkpoint into the LabRunner sub-step so the first full run happens only after both files exist. Alternatively keep file order but move the Run & See + checkpoint into sub-step 10 and end sub-step 9 with a compile-only checkpoint plus a NOTE box: "Do not run yet - bank.name has no default; we add application.yml next and then run." Update the sub-step 9 pitfall into that forward note. All pasted output stays as-is (it is truthful for the post-yaml state).

### F2: 20-hour step has no session plan, no time-boxes, no re-entry support

**Severity:** high
**Lens:** adhd
**Location:** Whole lesson; Orient "This Step in 30 Seconds" table (effort ≈20h) and every build checkpoint
**needsRun:** false
**Issue:** Effort is stated only as a single ≈20h figure. There is no plan of 6-10 sittings with named save points, no time estimate per movement or per sub-step, and checkpoints never say "stopping here? you have X working; next session starts at sub-step N, first action: ...". For an ADHD learner a monolithic 20h step with the first natural stopping guidance absent is the single biggest abandonment risk in an otherwise strong lesson.
**Fix:** (1) Add a "Suggested sittings" table to Orient, e.g.: S1 Orient+Understand (~2.5h), S2 Build 1-3 (~2h, save point: commit "RateProvider interface"), S3 Build 4-6 (~2.5h, save point: commit "LabConfig"), S4 Build 7-8 (~2h), S5 Build 9-10 + first full run (~2.5h), S6 Build 11 + Prove (~2.5h), S7 Play-with-it + Apply + Review (~3h). (2) Add "⏱ ~Xh" to each movement heading and "⏱ ~XX min" to each sub-step heading. (3) After the checkpoint of sub-steps 3, 6, 8, and 10 add a one-line re-entry note: "Stopping here? Everything committed compiles; next session starts at Sub-step N - first action: open <file> and ...".

### F3: The headline "flip" command runs a jar that has not been built yet

**Severity:** high
**Lens:** swe
**Location:** Sub-step 10 of 11, "Run & See - the flip" (lines ~1049-1060); also Play With It "One-command run"
**needsRun:** false
**Issue:** The learner has so far only run `spring-boot:run` and `compile`; no `package`/`verify` has executed, so `playground/spring-lab/target/spring-lab-0.1.0-SNAPSHOT.jar` does not exist and `java -jar ...` fails with "Unable to access jarfile" - at the exact moment the lesson promises its payoff ("the flip, with zero code change").
**Fix:** In sub-step 10's Run & See, either (a) prepend `./mvnw -pl playground/spring-lab -am package` with a one-line comment "build the executable jar first", or (b) replace the command with `./mvnw -pl playground/spring-lab spring-boot:run -Dspring-boot.run.arguments=--bank.rates.source=market` and keep the jar variant as an "after you've packaged" alternative. Apply the same guard to the Play With It "or, after a package build" line (already hedged there) and the Cheat Card (already runs verify first - fine).

### F4: False claim that plain Spring would not honor @ConditionalOnProperty

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 11 of 11, closing Pitfall (line ~1257)
**needsRun:** false
**Issue:** "Plain Spring (`@Conditional`) without Boot wouldn't honour `@ConditionalOnProperty`" is wrong: `@Conditional` evaluation is core Spring (`spring-context`), and `@ConditionalOnProperty` is just meta-annotated with `@Conditional(OnPropertyCondition.class)`. Any annotation-config Spring context evaluates it as long as spring-boot-autoconfigure is on the classpath. Teaching this as fact plants a misconception the interview-prep section would then reinforce.
**Fix:** Rewrite the pitfall to: "`@ConditionalOnProperty` ships in spring-boot-autoconfigure, not core Spring - it works in any Spring context that has Boot on the classpath, because condition evaluation itself is core Spring. What `ApplicationContextRunner` adds is the ergonomic test harness (`withPropertyValues`, context assertions) - don't confuse the annotation's origin with where it can run."

### F5: Run-and-see evidence missing for 7 of 11 build sub-steps

**Severity:** medium
**Lens:** structure
**Location:** Sub-steps 1, 3, 4, 5, 6, 7, 8 - checkpoint lines ("compiles", "both compile")
**needsRun:** true
**Issue:** The micro-anatomy contract requires run-and-see (exact command + expected output + common-wrong-output) for every sub-step. Only sub-steps 9-11 have it; sub-steps 1 and 3-8 end with an unverifiable "compiles" checkpoint, and only sub-step 2 even names the compile command. A beginner has no way to confirm progress for roughly the first two-thirds of the build.
**Fix:** Add to each of sub-steps 1 and 3-8 a minimal run-and-see: command `./mvnw -q -pl playground/spring-lab -am compile` (sub-step 1: `./mvnw -q -pl playground/spring-lab -am validate`), the real captured tail (e.g. the BUILD SUCCESS line), and one common-wrong-output (e.g. `cannot find symbol` -> wrong package/import; `Non-resolvable parent POM` -> wrong relativePath). Capture the outputs by actually running at each stage - do not fabricate.

### F6: First visible run happens hours into the build (first-win-fast violated)

**Severity:** medium
**Lens:** adhd
**Location:** Sub-step 2 of 11, Predict block (line ~414)
**needsRun:** true
**Issue:** Nothing observably runs until sub-step 9's full app run; sub-steps 1-8 are file-writing plus (implied) compiles. Sub-step 2 even poses "if you run spring-boot:run right now, will it start?" as a thought experiment with a hidden answer - the perfect first win, left un-run. The reward loop is empty for the longest stretch of the lesson.
**Fix:** Promote sub-step 2's predict into a real Run & See: after the predict, instruct `./mvnw -pl playground/spring-lab spring-boot:run`, paste the real captured output showing the context starting and the JVM exiting immediately (with the Spring log lines), and add a one-line "you just booted an IoC container with zero beans of your own - first win" checkpoint. Capture real output; do not fabricate.

### F7: No scaffold fading - every sub-step is fully worked, zero type-it-yourself

**Severity:** medium
**Lens:** pedagogy
**Location:** Whole build (sub-steps 1-11)
**needsRun:** false
**Issue:** All 11 sub-steps hand the learner complete code to paste. The interactivity toolkit's type-it-yourself element never appears, so the learner never retrieves/generates during the build; generation is deferred entirely to end-of-lesson exercises. Later sub-steps that mirror earlier ones are ideal fading candidates and are wasted.
**Fix:** Convert two mirrors into type-it-yourself: (1) sub-step 4's `MarketRateProvider` - give the spec ("same shape as FixedRateProvider but: havingValue market, no matchIfMissing, hardcoded `new BigDecimal(\"0.0475\")`, name() market") and hide the full file in a `<details>` solution block; (2) sub-step 11's `MarketRateContextTest` - "write it yourself from SpringLabApplicationTests, changing the property and the two expected values", solution hidden. Keep the visible line-by-line for both.

### F8: Understand movement has no knowledge-checks across ~1,600 words

**Severity:** medium
**Lens:** pedagogy
**Location:** B Understand - especially "Under the Hood" 8-item numbered list (lines ~155-173)
**needsRun:** false
**Issue:** Between the Big Idea and the Build there is no retrieval or self-check: eight dense conceptual paragraphs (BeanDefinition, constructor selection, proxyBeanMethods, scopes, conditions, SpEL, lifecycle) arrive back-to-back with nothing to interrupt passive reading, and the terms all reappear later assuming they stuck. (Also a working-memory nit: the thread-safety note at line ~210 references `AuditEntry` before that class has been introduced anywhere.)
**Fix:** Insert a "⚡ Quick check (30 seconds)" block after item 8 with 3 hidden-answer questions: "A BeanDefinition is the ___, not the ___" ; "When are @ConditionalOnProperty conditions evaluated - before or after instantiation?" ; "In full-mode @Configuration, beanB() calling clock() returns what?". Add one more after Then-vs-Now ("what replaced XML wiring, in order?"). In the thread-safety note, change "Note `AuditEntry` keeps..." to "Note: the prototype bean we'll build in Sub-step 8 (`AuditEntry`) keeps...".

### F9: Optional content carries no time-cost labels

**Severity:** low
**Lens:** adhd
**Location:** E Apply - four "Go Deeper" details blocks, the stretch goal, and Play With It's break-it experiments
**needsRun:** false
**Issue:** Optional material is well-marked as optional but never says what it costs, so a time-blind learner cannot budget: the stretch goal (a @ConfigurationProperties conversion keeping 6 tests green) is easily 30-60 min but looks the same size as a 3-minute read.
**Fix:** Append time labels to each: Go Deeper summaries "(~4 min read)"; break-it experiments intro "(~15-20 min for all three)"; stretch goal "(~45-60 min)".

### F10: Sub-step 5 break-it aside describes code that would not compile

**Severity:** low
**Lens:** swe
**Location:** Sub-step 5 of 11, "🔬 Break-it" paragraph (line ~645)
**needsRun:** false
**Issue:** "If you delete the constructor parameter ... the app still starts" skips that the `final RateProvider rateProvider` field would then be unassigned - a compile error, not a running app - unless you also stub the field, which the aside doesn't say (the Play With It version does).
**Fix:** Reword to match the Play With It version: "delete the constructor parameter and temporarily stub the field (`this.rateProvider = new FixedRateProvider(new BigDecimal(\"0.0325\"))`) - the app starts but you've hard-coupled the service again; restore constructor injection", or simply replace the aside with a pointer: "we'll do this for real in Play With It, break-it #2."
