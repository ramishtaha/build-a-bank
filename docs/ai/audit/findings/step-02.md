# Step 2 audit - swe:8 pedagogy:8 adhd:5 structure:9 - thinBuild:false

## Strengths

- Exemplary build micro-anatomy: all 12 sub-steps carry the full Goal/Location/complete-code-with-path-headers/line-by-line/under-the-hood/predict/run-and-see/checkpoint/commit/pitfall chain, plus high-quality break-it-on-purpose experiments (exhaustiveness compile error, double-drift demo, rounding-mutation sanity check).
- Technically strong and honest: complete compile-plausible code (all imports, package lines), correct Java version history (JEP numbers all check out), correct money (`BigDecimal`/`HALF_EVEN`/String construction) and time (`Instant` UTC) treatment, and an explicit, honest note about why there is no `requests.http`/HTTP/DB this step.
- Full spine with strong retrieval practice: all six movements present, mini-TOC anchors resolve, verification tier stated, 6 interview Q&As, 5 test-yourself items, 5 flashcards, opening build-map + files tree and closing sequence diagram.

## Missing spine

none

## Findings

### F1: No session plan for a ~20-hour step

**Severity:** high
**Lens:** adhd
**Location:** A · Orient — "This Step in 30 Seconds" table / "Before You Start" (lines ~29-118)
**needsRun:** false
**Issue:** The step is estimated at ~20 hours but there is no planned-sittings structure anywhere — no 6-10 named sittings of ~2-3h, no save points. An ADHD learner facing "20 hours" with 12 sub-steps and no partition has no scaffold for starting, stopping, or resuming; the only granularity offered is the whole step.
**Fix:** Add a "🗓️ Suggested sittings" table at the end of Orient (after Before You Start) with ~8 rows, e.g.: S1 (~2h) Movements A+B, save point = none needed; S2 (~2.5h) sub-steps 1-3, save point = commit "Customer record"; S3 (~2.5h) sub-steps 4-5, save point = commit "pattern-matching switch"; S4 (~2.5h) sub-steps 6-8; S5 (~2.5h) sub-steps 9-10; S6 (~2h) sub-step 11 + Play With It; S7 (~2.5h) sub-step 12 + Movement D; S8 (~2h) Movements E+F. Name each save point as the sub-step's existing commit message.

### F2: Verification Log output format is not what Surefire emits ("real, pasted, unedited" claim in doubt)

**Severity:** high
**Lens:** swe
**Location:** D · Prove, item 1 (lines ~1730-1741); same block in Sub-step 12 "Run & See" (lines ~1603-1614); related claim in sub-step 12 line-by-line ("you'll see it in the verify log") and Play With It item 2
**needsRun:** true
**Issue:** The line `[INFO] Tests run: 5, ... -- in MoneyTest (DisplayName: "Money — exact decimal money arithmetic")` is not a format Maven Surefire produces. Default Surefire prints the fully-qualified class name (`-- in com.buildabank.basics.money.MoneyTest`); with phrased-name reporting configured it prints the display name alone. The reactor line `... Java Basics .......... SUCCESS` also lacks Surefire's timing suffix (`SUCCESS [ 2.3 s]`). This contradicts the log's "All output below is real, pasted, unedited" promise and undermines Movement D's whole purpose.
**Fix:** Re-run `./mvnw -B verify` at `step-02-end` and paste the genuine tail into both the Sub-step 12 expected-output block and the Prove section, replacing the invented `(DisplayName: ...)` lines. Also correct the two prose claims that the display name appears in the verify log if the real output shows the FQCN instead.

### F3: `-q` runs promise a "BUILD SUCCESS" line that quiet mode suppresses

**Severity:** medium
**Lens:** swe
**Location:** Sub-steps 1-10, every "▶️ Run & See" using `./mvnw -B -q ... validate/compile` (first at lines ~380-392, repeated through line ~1207)
**needsRun:** false
**Issue:** All ten compile/validate run-and-sees state "✅ Expected output: a quiet `BUILD SUCCESS` (with `-q`, you mostly see just the success line and timing)". Maven `-q` shows only errors — on success it prints nothing at all. A nervous beginner runs the command, sees zero output, and has no way to tell success from a hang or a swallowed failure; the stated expected output is simply wrong.
**Fix:** Pick one convention and apply it to all ten blocks: either (a) remove `-q` from the compile/validate commands and keep "expect a `[INFO] BUILD SUCCESS` tail", or (b) keep `-q` and rewrite each expected output to "no output at all — with `-q`, silence IS success; any red `[ERROR]` lines mean failure (run again without `-q` to see detail)". Update sub-step 1's flag explanation to match.

### F4: "Byte-for-byte" demo determinism claim is false for the `counts by type` line

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 11 — 🔮 Predict, ✅ Expected output, and ✋ Checkpoint ("byte-for-byte", lines ~1314-1381); "fully deterministic" claim in 💭 Under the hood (line ~1312); Prove section item 2
**needsRun:** true
**Issue:** `countByType` collects into `Collectors.groupingBy`'s default `HashMap`. Enum `hashCode()` is identity-based and varies per JVM run, so `{CREDIT=1, DEBIT=2}` can legitimately print as `{DEBIT=2, CREDIT=1}` on another run. The predict answer even hedges ("prints in this order here") while the checkpoint demands the report match "byte-for-byte" and under-the-hood calls the output "fully deterministic" — internally inconsistent and a trap for a learner whose correct build prints the other order.
**Fix:** Change `countByType` to `Collectors.groupingBy(Transaction::type, () -> new EnumMap<>(TransactionType.class), Collectors.counting())` (add `java.util.EnumMap` import and one line-by-line bullet explaining EnumMap iterates in declaration order, making the output stable), re-run the demo and `verify`, and refresh the pasted outputs. Alternatively, keep the code and soften the checkpoint/under-the-hood wording to say the counts line may print in either order — but the EnumMap fix is the better teach.

### F5: Checkpoints lack re-entry support

**Severity:** medium
**Lens:** adhd
**Location:** ✋ Checkpoint blocks of sub-steps 1-12 (e.g. lines ~392, 512, 585, 679, 762, 844, 907, 1018, 1128, 1209, 1381, 1620)
**needsRun:** false
**Issue:** Checkpoints confirm state ("X compiles") but never tell a stopping learner what they have, where the next session starts, or the first command to run on return. For a 20-hour, multi-sitting step this forces re-orientation from scratch each sitting — a classic ADHD re-entry failure.
**Fix:** Append a one-line re-entry note to the checkpoints at the natural sitting boundaries (sub-steps 3, 5, 8, 10, 11), e.g. after sub-step 5: "🛑 Stopping here? You have the whole sealed Account family + exhaustive switch committed and green. Next session: Sub-step 6 (Transaction + enum) — first action: `./mvnw -B -pl playground/java-basics -am -DskipTests compile` to confirm you're still green." Align these with the F1 sitting plan.

### F6: Nine consecutive sub-steps whose only feedback is a silent compile

**Severity:** medium
**Lens:** adhd
**Location:** Sub-steps 2-10 "▶️ Run & See" blocks (lines ~502-1207)
**needsRun:** true
**Issue:** Every sub-step from Money (2) through TimeExamples (10) ends with the identical `compile` command and (per F3, literally silent) BUILD SUCCESS. The first time the learner sees their code produce a visible value is Sub-step 11 — potentially 10+ hours in. Reward-loop density through the core of the build is near zero: type ~600 lines, see nothing but exit codes.
**Fix:** Insert a 2-minute "🎮 See it live in jshell" interlude after sub-steps 2, 5, and 9: `jshell --class-path playground/java-basics/target/classes`, then e.g. `Money.of("2.125", "USD")` (banker's rounding visible), `AccountInfo.describe(new CheckingAccount(...))`, and `TransactionAnalytics.net(...)`. Run each snippet for real and paste the actual jshell output (`$1 ==> ...`) as the expected result — do not invent it.

### F7: No scaffold fading — all 12 sub-steps are fully worked copy-paste

**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, sub-steps 1-12 overall; most acutely sub-step 12's four complete test classes (lines ~1400-1580)
**needsRun:** false
**Issue:** Every file, including the final tests, is given as a complete listing. By the contract, later sub-steps should shift toward type-it-yourself so the learner produces (not just transcribes) code; a learner can go green here without ever writing a line unaided, which weakens the step's own DoD claim "explain in your own words."
**Fix:** Convert sub-step 10 (`TimeExamples`) and one test class in sub-step 12 (`InMemoryCustomerRepositoryTest` is the easiest) into type-it-yourself: give the Javadoc + method signatures (or the test names + what each must assert) as the spec, instruct the learner to write the bodies, and put the full reference listing in a collapsed `<details><summary>Compare with the reference</summary>` block. The compile/verify run-and-see already validates their attempt.

### F8: Sub-step 9 introduces ~8 new stream tokens in one bite

**Severity:** low
**Lens:** pedagogy
**Location:** Sub-step 9 — TransactionAnalytics (lines ~1031-1137)
**needsRun:** false
**Issue:** One sub-step introduces streams, lambdas, `filter`, `map`, `reduce`, method references, `groupingBy`+`counting`, `max`+`Comparator`, and `toList` — roughly triple the ~3-new-terms budget. The single knowledge-check at the end can't carry that load.
**Fix:** Split the code entry into two typed passes with a compile between: pass 1 = `totalAmount` + `net` (stream/filter/map/reduce/method reference) ending in a knowledge-check ("which op is terminal and why does nothing run before it?"); pass 2 = `countByType`/`largest`/`since` (collectors, Comparator, Optional). Keep it one sub-step (9a/9b) so the 12-step map and files tree stay valid.

### F9: No time-boxes below the whole-step level

**Severity:** low
**Lens:** adhd
**Location:** Movement headings A-F and all sub-step headings (lines 27, 123, 250, 319-1394, 1724, 1777, 1880)
**needsRun:** false
**Issue:** The only durations given are the whole step (~20h) and the 60/90-second break-its. A learner cannot budget a sitting or notice they're overrunning a sub-step (a key ADHD time-blindness support).
**Fix:** Add an "≈ time" tag to each movement heading (e.g. "B · 🧠 Understand *(≈ 2h)*") and each sub-step heading (e.g. "Sub-step 2 of 12 — Money *(≈ 75-90 min)*"), with numbers consistent with the F1 sitting plan summing to ~20h.

### F10: Optional content not labeled with its time cost

**Severity:** low
**Lens:** adhd
**Location:** E · Apply — the four 🚀 Go Deeper `<details>` (lines ~1779-1821); 💡 "Faster in IntelliJ" tip (line ~1690)
**needsRun:** false
**Issue:** Optional asides are marked optional but carry no time estimate, so a learner can't decide whether to detour without opening them (and risk a rabbit hole).
**Fix:** Add a time tag to each `<summary>`: javap (~10 min), record invariants (~5 min), when-not-streams (~5 min), Optional-as-return-type (~5 min), and "(2 min)" on the IntelliJ tip.

### F11: run-and-see lacks a common-wrong-output in six sub-steps

**Severity:** low
**Lens:** structure
**Location:** Sub-steps 3, 6, 7, 8, 9, 10 — "▶️ Run & See" blocks (lines ~577-583, 836-842, 899-905, 1010-1016, 1118-1124, 1201-1207)
**needsRun:** true
**Issue:** The micro-anatomy contract requires exact command + expected output + common-wrong-output for every sub-step; these six give only the command and "quiet BUILD SUCCESS". Sub-steps 1, 2, 4, 5, 11, 12 show what the contract intends.
**Fix:** Add one ❌ block per listed sub-step showing the most likely real failure (e.g. sub-step 6/8: forgetting the `com.buildabank.basics.money.Money` import → `cannot find symbol: class Money`; sub-step 9: a mistyped method reference). Reproduce each error once and paste the genuine `[ERROR]` line rather than inventing compiler text; where the failure mode is identical to sub-step 2's, a one-line pointer ("same failure modes as sub-step 2's ❌") is acceptable.
