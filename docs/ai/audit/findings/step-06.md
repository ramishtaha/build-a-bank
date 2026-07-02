# Step 6 audit - swe:7 pedagogy:8 adhd:5 structure:9 - thinBuild:false

## Strengths

- Complete six-movement spine with resolving anchors, a stated 🟠 Standard verification tier, and lesson artifacts that match the repo byte-for-byte at `step-06-end` (LabRunner, GreetingAutoConfiguration, `.imports`, hello-service `application.yml` all verified against the tag).
- High interactivity density: predicts with hidden answers in 4 of 6 sub-steps, a knowledge-check, three break-it experiments, per-sub-step you-are-here breadcrumbs, per-sub-step commits, and opening build-map + closing sequence Mermaid diagrams with alt-text.
- Strong conceptual teaching: the serviced-apartment analogy, genuine under-the-hood mechanics (ordering of auto-configs vs user beans), the `spring.factories` → `.imports` version story, and 6 interview Q&A that map directly onto what was built.

## Missing spine

none

## Findings

### F1: Sub-step 2 hides the actual @Value removal - the refactor's diff is incomplete and mislabeled

**Severity:** high
**Lens:** swe
**Location:** Sub-step 2 of 6, "Edit (key parts...)" block, lines ~486-525
**needsRun:** false
**Issue:** The Step-5 `LabRunner` constructor (verified at tag `step-05-end`) has `@Value("${bank.name}") String bankName` and `@Value("#{ ${bank.rates.fixed:0.0325} * 100 }") double ratePercent` parameters. The lesson's whole point is "refactor LabRunner off `@Value`/SpEL", yet the edit shows only an elided AFTER fragment (`// ...`) with "NEW" annotations: it never shows the BEFORE constructor, never instructs the learner to delete the two `@Value` params, the `org.springframework.beans.factory.annotation.Value` import, or the old rate log line using `ratePercent`. The line-by-line even says the banner "was a literal" (false - it was `@Value("${bank.name}")`), and the header comment says "(pass 2 of 2)" while the prose calls this pass 2 of a 2/5 pair - sub-step 5 is the real second pass. A learner pasting the fragment on top of Step-5 code gets a file that does not compile (duplicate/leftover params).
**Fix:** Replace the fragment with a true before → after diff of the constructor + run() method: BEFORE showing the two `@Value` params and the old banner/rate lines, AFTER showing the 4-param constructor and new lines; add an explicit bullet "DELETE the two `@Value` constructor params and the now-unused `import org.springframework.beans.factory.annotation.Value;`". Change "was a literal" to "was injected via `@Value("${bank.name}")` - the pattern we're retiring", and change the code-header comment "(pass 2 of 2)" to "(pass 1 of 2 - pass 2 lands in sub-step 5)".

### F2: False checkpoint claim that the app can't run yet because bank.name isn't set

**Severity:** high
**Lens:** swe
**Location:** Sub-step 2 checkpoint (line ~540) and sub-step 5 prose (line ~864)
**needsRun:** false
**Issue:** The checkpoint says "It won't *run* fully yet because we haven't set `bank.name` - we'll provide it via the run args / config in sub-step 5". Verified false: `bank.name: Build-a-Bank` and `bank.rates.*` have been in `playground/spring-lab/src/main/resources/application.yml` since `step-05-end` (Step 5's `@Value("${bank.name}")` had no default, so it *required* it), and the pass-1 LabRunner doesn't need `GreetingService` yet - the app would run fine right here. Sub-step 5's "we set `bank.name` so the banner and greeting read 'Build-a-Bank'. The smoke test and the app's own application.yml/run args provide these" is equally vague/misleading - the lesson never shows where these values live, so a nervous learner can't verify their config matches.
**Fix:** In sub-step 2, correct the checkpoint to "the app already runs - `bank.*` has been in the lab's `application.yml` since Step 5" (optionally invite an early `java -jar` run for an extra reward loop). In sub-step 5, replace the vague sentence with a quoted micro-recap of the existing `bank:` block from `playground/spring-lab/src/main/resources/application.yml` ("already there from Step 5 - confirm yours matches").

### F3: No session plan or re-entry support for a 20-hour step

**Severity:** high
**Lens:** adhd
**Location:** A · Orient (effort row, line ~35) and all sub-step checkpoints
**needsRun:** false
**Issue:** The step is estimated at ≈20 focused hours but has zero sitting structure: no planned sessions with named save points, and no re-entry lines at checkpoints ("stopping here? you have X working; next session starts at sub-step N, first action: ..."). Per-sub-step commits exist but are never framed as save points, so an ADHD learner returning after 3 days has no anchor.
**Fix:** Add a "📅 Session plan" box at the end of Orient splitting the step into ~7 sittings of 2-3h, each ending at an existing commit (e.g. S1: Orient+Understand; S2: sub-step 1; S3: sub-step 2-3; S4: sub-step 4; S5: sub-step 5 + break-its; S6: sub-step 6 + Play With It; S7: Prove+Apply+Review+flashcards). Append a one-line re-entry note to each sub-step's ✋ Checkpoint: "Stopping here? You've committed <artifact>; next session opens at Sub-step N - first action: <command/file>."

### F4: Sub-step 4 break-it makes an unverified (likely wrong) claim about verify staying green

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 4, "🔬 Break-it (60s) - make the discovery file lie" (line ~765)
**needsRun:** true
**Issue:** It says: rename the `.imports` file, rerun `./mvnw -pl playground/spring-lab -am verify`, and "watch `registersGreetingServiceByDefault` still pass". But the module also has `@SpringBootTest`-based tests (`SpringLabApplicationTests`, `BankPropertiesTest`) that boot the full context, which must instantiate the `@Component` `LabRunner` - whose constructor requires a `GreetingService`. With discovery broken, those contexts fail to start, so `verify` goes RED with `UnsatisfiedDependencyException`, not green. A learner told "the test still passes" who sees a failing build will assume they broke something else.
**Fix:** Actually run the experiment against `step-06-end`, paste the real outcome, and rewrite the break-it to teach it honestly: e.g. run only the runner test (`./mvnw -pl playground/spring-lab -am test -Dtest=GreetingAutoConfigurationTest`) to show it still passes, then run full `verify` to show the `@SpringBootTest` contexts now fail with the pasted `UnsatisfiedDependencyException` - which is itself the lesson ("being an auto-config" vs "being discovered").

### F5: Sub-step 6 expected output doesn't match the commands, and four curls have no expected output

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 6 Run & See (lines ~977-1009) and Verification Log §3 (lines ~1129-1138)
**needsRun:** true
**Issue:** The run-and-see gives two `jq` commands that each print a bare number, but the ✅ Expected block shows a hand-formatted "captured summary" (`context id: hello-service / positiveMatches...: 141 / ...`) that no shown command produces - a learner comparing terminal output to the block sees a mismatch. The same paraphrased block is reused as "real pasted output" in the Verification Log. The four follow-up curls (`/configprops`, `/beans`, `/env`, `/mappings`) have no expected output at all, so there's no way to know what success looks like.
**Fix:** Re-run the commands against a live hello-service and paste literal outputs: `141` and `82` (or current values) as the expected output of the two jq commands, and one short pasted line per follow-up curl. In the Verification Log, either paste the raw command+output pairs or explicitly label the summary as derived and add at least one verbatim command/output pair.

### F6: No per-movement or per-sub-step time-boxes

**Severity:** medium
**Lens:** adhd
**Location:** "The Six Movements" TOC (lines ~12-22) and each "Sub-step N of 6" heading
**needsRun:** false
**Issue:** Only the whole-step estimate (≈20h) exists. There are no time-boxes on movements A-F or on the six sub-steps, so a learner cannot plan a sitting or know whether being 90 minutes into sub-step 4 is normal or a signal they're stuck.
**Fix:** Append an estimate to each TOC entry (e.g. "A Orient - 30 min · B Understand - 2.5h · C Build - 12h · D Prove - 1h · E Apply - 2.5h · F Review - 1.5h") and to each sub-step heading (e.g. "Sub-step 4 of 6 (≈ 2.5-3h)"). Numbers should sum to roughly the stated 20h.

### F7: No scaffold fading - every sub-step stays fully worked, zero type-it-yourself

**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, sub-steps 3-6
**needsRun:** false
**Issue:** All six sub-steps present complete copy-paste code. The interactivity toolkit's type-it-yourself element never appears, and later sub-steps are as fully worked as sub-step 1, so the learner never retrieves the patterns unaided before the Your-Turn exercises.
**Fix:** Fade the scaffold in the last third: in sub-step 4, give the test class skeleton plus the first test, then have the learner write `backsOffWhenDisabled` and `backsOffWhenUserDefinesOwnBean` themselves from the described behaviors (full solutions in a collapsed `<details>`); in sub-step 4 also have them author the one-line `.imports` file content from the naming rule before revealing it.

### F8: Micro-anatomy gaps: missing under-the-hood (sub-steps 2, 6), missing predict (sub-step 3), no common-wrong-output anywhere

**Severity:** medium
**Lens:** structure
**Location:** Sub-step 2 (~lines 482-548), sub-step 3 (~552-610), sub-step 6 (~912-1019), all ▶️ Run & See blocks
**needsRun:** false
**Issue:** The contract requires the full micro-anatomy per sub-step. Sub-step 2 and sub-step 6 have no 💭 under-the-hood; sub-step 3 has no 🔮 predict-then-run; and no run-and-see block in the lesson carries a common-wrong-output (failure signatures live only in end-of-sub-step pitfalls and the troubleshooting table).
**Fix:** Add a 💭 paragraph to sub-step 2 (how constructor injection resolves the bound record) and sub-step 6 (how `exposure.include` filters which endpoint beans get web-mapped); add a 🔮 to sub-step 3 ("will `context.getBean(GreetingService.class)` succeed right now? why not?"); and under each ▶️ Run & See add one "❌ Common wrong output" line reusing existing troubleshooting content (e.g. `NoSuchBeanDefinitionException: BankProperties` → forgot `@EnableConfigurationProperties`).

### F9: Hard-coded Actuator match counts (141/82) presented as the expected result

**Severity:** low
**Lens:** adhd
**Location:** Sub-step 6 expected block (~line 989), sequence diagram (~line 1043), Verification Log §3
**needsRun:** false
**Issue:** The counts 141/82 are machine-and-version specific (they shift with any Boot patch or dependency change), but they appear under a ✅ Expected marker. A nervous learner seeing 143/80 has no way to know that's fine and will assume they misconfigured something.
**Fix:** Add one reassurance line after the expected block: "Your exact counts may differ by a few (they depend on the Boot patch version) - what matters is that BOTH sets are non-empty and `DataSourceAutoConfiguration` appears in negativeMatches."

### F10: Optional content has no time-cost labels

**Severity:** low
**Lens:** adhd
**Location:** "🚀 Go Deeper (Optional)" (~line 1162), break-its #2/#3 (~lines 898-900), Stretch exercise (~line 1246)
**needsRun:** false
**Issue:** Optional material is marked optional but never carries its time cost. Break-it #2 requires a code edit + rebuild + revert; the stretch adds a dependency and tests - an ADHD learner can't budget whether to take the detour.
**Fix:** Label each: "Go Deeper (optional - 4 asides, ~10 min each)", "Break-it #2 (~10 min, edit + rebuild + revert)", "Break-it #3 (~2 min)", "Stretch (~45-60 min, reference solution in solutions/step-06/)".
