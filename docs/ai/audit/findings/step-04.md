# Step 4 audit - swe:8 pedagogy:8 adhd:6 structure:9 - thinBuild:false

## Strengths

- The sacred build is fully honored: all 7 sub-steps carry the complete micro-anatomy (goal, location, complete code with path headers, line-by-line, under-the-hood, predict, run-and-see, checkpoint, commit, pitfall), and the marquee escape-analysis experiment is real — this audit independently reproduced it on Java 25.0.3 (0 GC pauses non-escaping vs 18 with the ring buffer; checksum 637493856 matches the lesson's math).
- Verification Log is genuinely captured, tier-stated (Standard), with honest nondeterminism caveats; all six mini-TOC anchors resolve; every spine section is present, and the absence of requests.http is explicitly justified in-lesson (the play surface is the JVM CLI).
- Excellent misconception handling and interview framing (PermGen trap question, reachable-vs-unused leak, JFR samples-vs-totals), with concrete callbacks to Steps 1-3 and forward links to Steps 11 and 55.

## Missing spine

- none (all six movements and required subsections present; requests.http intentionally absent and justified in the Build intro note; smoke.sh exists)

## Findings

### F1: No session plan or re-entry support for a 15-20 hour step

**Severity:** high
**Lens:** adhd
**Location:** A - Orient, "This Step in 30 Seconds" table (lines 29-38) and all Build checkpoints (sub-steps 1-7)
**needsRun:** false
**Issue:** The step declares 15-20h effort but has no planned sittings, no named save points, and no re-entry lines at checkpoints. An ADHD learner returning after a break has no "you have X working; next session starts at sub-step N, first action: ..." guidance anywhere.
**Fix:** Add a "Session plan" box at the end of Orient with 6-8 sittings of ~2-3h each, e.g.: S1 Orient + Understand 1-3 (~2h), S2 Understand 4-6 + security/then-vs-now (~2h), S3 Build sub-steps 1-2 (~2.5h, save point: both classes committed), S4 sub-step 3 GC + escape experiment (~2h, save point: escape surprise seen), S5 sub-steps 4-5 JIT + JFR (~2.5h), S6 sub-steps 6-7 + Prove (~2.5h, save point: step-04-end tagged), S7 Apply + Review (~2h). Then append one re-entry line to each build checkpoint, e.g. after sub-step 3: "Stopping here? You have GC observation working and the escape lesson done; next session starts at sub-step 4, first action: run the -XX:+PrintCompilation command."

### F2: Referenced solutions/step-04/ does not exist in the repo

**Severity:** high
**Lens:** swe
**Location:** E - "Your Turn: Practice & Challenges", Stretch block (lines 1184-1190)
**needsRun:** true
**Issue:** The lesson says twice "Reference solutions live in `solutions/step-04/`", but the repo has only `solutions/step-01/` — the promised BoxingBytecode, PromotionDemo, and EscapeBenchmark reference solutions do not exist. A learner who attempts the stretch exercises and gets stuck has nowhere to look.
**Fix:** Create `solutions/step-04/` containing working, compiled-and-run BoxingBytecode.java, PromotionDemo.java, and an EscapeBenchmark JMH skeleton (verify each runs and shows the claimed behavior: Integer.valueOf in javap output, old-gen growth then OOME under -Xmx128m, and a measurable escaping-vs-non-escaping delta). If solutions are deliberately deferred, edit both references to say "solutions land with the Phase A solutions pack; until then compare against `git checkout step-04-end`".

### F3: No time-boxes per movement, per sub-step, or on optional content

**Severity:** medium
**Lens:** adhd
**Location:** All movement headings (A-F) and Build sub-step headings 1-7; optional experiments in sub-steps 4-6; Go Deeper details (lines 1110-1132)
**needsRun:** false
**Issue:** The only time information is the whole-step 15-20h estimate. Individual movements, sub-steps, optional break-it experiments, go-deeper details, and stretch exercises carry no time cost, so a learner cannot budget a sitting or decide whether to take an optional detour.
**Fix:** Append an estimate to each movement heading ("B - Understand (~3h)") and each sub-step heading ("Sub-step 3 of 7 (~2h incl. the escape experiment)"), and label every optional block with its cost: "Optional experiment (~10 min)", each Go Deeper details summary "(~5 min read)", each stretch exercise "(~45-90 min)".

### F4: Truncated captured output presented as complete

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 3 expected output (lines 596-605); sub-step 5 JFR summary (lines 749-760); Verification Log sections 3 and 5 (lines 1046-1082)
**needsRun:** false
**Issue:** The GC block shows exactly 5 pauses and stops; a real run (reproduced during this audit) prints ~18 `Pause Young` lines and ends with the program's own `Allocated 5,000,000 blocks ...` stdout line. The `jfr summary` block shows only 4 event types, but the real command prints a table with dozens of jdk.* rows. Neither block says it is an excerpt (unlike the PrintCompilation block, which honestly says "filtered from the full firehose"). A learner diffing their output against the lesson will see far more and may conclude something is wrong.
**Fix:** Add "(first 5 lines shown — the full run continues with ~15+ similar pauses and ends with the `Allocated 5,000,000 blocks ...` line)" under both GC blocks, and "(excerpt — the full table lists dozens of jdk.* event types; these four are the ones to find)" under both JFR blocks.

### F5: No scaffold fading — zero type-it-yourself in the entire build

**Severity:** medium
**Lens:** pedagogy
**Location:** C - Build, sub-step 7 (lines 833-916)
**needsRun:** false
**Issue:** Every code artifact is fully worked; the interactivity toolkit's type-it-yourself element never appears, so scaffolding never fades. By sub-step 7 the learner has seen AssertJ/JUnit twice before (Steps 2-3) and should be writing the test, not pasting it.
**Fix:** Convert sub-step 7 to type-it-yourself: give the test-class skeleton plus the completed `bytecodeSampleComputesCorrectly`, then instruct "now write `allocationChecksumIsDeterministic` yourself: call `run(256)` and assert it equals 32640L (the comment should prove the math: 0+1+...+255 = 32640)", with the full listing moved into a collapsed `<details>` labeled "stuck? full listing".

### F6: Build commands are bash-only while the lesson claims Windows support

**Severity:** medium
**Lens:** swe
**Location:** Cheat Card (lines 61-85), sub-step 3 command (lines 581-584), Play With It table and TIP (lines 943-966)
**needsRun:** false
**Issue:** The cheat card says "Windows uses `.\mvnw.cmd`" — implying PowerShell/cmd — but every subsequent command uses bash-only syntax: `CP=playground/...` env assignment, `"$CP"` expansion, `| grep`, `| grep -c`, `| head -40`. In PowerShell `CP=...` is a parse error and grep/head do not exist, so a Windows learner following literally is stuck at sub-step 3.
**Fix:** Add one note at the top of the Build: "On Windows, run every command in this step from Git Bash (installed with Git in Step 1) — the `CP=...`/`$CP`/`grep` syntax is bash. PowerShell equivalents: `$CP='playground/java-basics/target/classes'`, use `-cp $CP`, and replace `| grep X` with `| Select-String X` and `| grep -c X` with `(... | Select-String X).Count`."

### F7: Sub-step 2 expected output is a placeholder, not captured output

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 2, Run & See expected output (lines 552-558)
**needsRun:** true
**Issue:** The expected output reads `Allocated 5,000,000 blocks in <a few> ms (checksum=...)` — a template, violating the contract's real-pasted-output rule. The checksum is fully deterministic (the whole point of the design, and the thing the test pins), so eliding it wastes the first chance to show determinism; only the ms varies.
**Fix:** Run `java -cp playground/java-basics/target/classes com.buildabank.basics.jvm.AllocationDemo 5000000` and paste the real line (checksum will be 637493856 — verified during this audit), annotated "your ms will differ; the checksum must not". Also add the missing common-wrong-output note: "checksum different -> you edited the loop math; re-check `i & 0xFF`."

### F8: Six flashcards exceed the 3-5 contract

**Severity:** low
**Lens:** structure
**Location:** F - Recap, section (g) Flashcards (lines 1293-1313)
**needsRun:** false
**Issue:** The contract caps flashcards at 5; there are 6, and the last card ("What is G1, OSR, and deoptimization?") bundles three unrelated facts, making it a weak retrieval item anyway.
**Fix:** Delete the sixth card and move its G1 fact into card 3's answer ("...plus the heap's young gen is collected by G1 evacuation pauses"); OSR/deopt are already covered by test-yourself and interview Q4, leaving exactly 5 well-scoped cards.

### F9: Wall-of-text in Understand sections 1-2

**Severity:** low
**Lens:** adhd
**Location:** B - Under the Hood, "1. javac: source -> bytecode" and "2. The java launcher" (lines 172-180)
**needsRun:** false
**Issue:** Roughly 300 words of continuous prose with no code, diagram, or interactive break between the Big Idea diagram and the classloader diagram — the longest unbroken run in the lesson.
**Fix:** Break section 1 with a 4-line teaser snippet of the `add` disassembly (`iload_0 / iload_1 / iadd / ireturn` — "you'll produce this yourself in sub-step 1") and convert section 2's parenthetical about JARs into a one-row aside or bullet list ("a .jar is: a ZIP of .class files + MANIFEST.MF; java -jar reads Main-Class").

### F10: Sub-step 1 introduces ~12 opcodes at once with no legend

**Severity:** low
**Lens:** pedagogy
**Location:** Sub-step 1, expected output + "Read it with me" (lines 415-453)
**needsRun:** false
**Issue:** The disassembly confronts the learner with iload/iadd/ireturn/lconst/lstore/iconst/istore/if_icmpgt/i2l/ladd/iinc/goto in one block — far past the ~3-new-terms guideline — and the type-prefix rule (i=int, l=long, a=reference) is only stated back in Understand, a long scroll away.
**Fix:** Insert a 5-row mini-legend table immediately before the expected output (prefix i/l/a = int/long/reference; load/store = push/pop a local; const = push a constant; if_/goto = branch; iinc = increment local in place), and add the one-line micro-recap "remember from Understand: bytecode pushes/pops an operand stack, no registers".
