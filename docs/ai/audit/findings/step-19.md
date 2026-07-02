# Step 19 audit - swe:6 pedagogy:5 adhd:4 structure:5 - thinBuild:true

## Strengths

- Technically accurate, interview-grade theory: CAP correctly scoped to partition behavior, PACELC's else-branch, W+R>N with the W>N/2 nuance, Lamport vs vector clock limits, and exactly-once *effect* vs delivery (with FLP) are all stated correctly and without hand-waving.
- Honest Verification Log: a real mutation run (drop the CP partition guard, watch `CapPacelcTest` fail, revert) plus an explicit §12.8 honesty note that the labs are in-process simulations, not a real cluster.
- Strong prior-step callbacks with one-line recaps (Step 11 concurrency-lab idiom, Step 12 single-node consistency, Step 14 idempotency), and a single no-infra run command (`./mvnw -pl playground/distributed-lab test` / `make play-19` — both verified to exist in the repo).

## Missing spine

- Complete code for every build sub-step: none of the 5 production classes, 4 test classes, or the 2 pom.xml changes are shown anywhere in the lesson (the code exists in the repo at `playground/distributed-lab` but is invisible to the reader).
- Per-sub-step line-by-line explanations and under-the-hood sections (absent in all 5 sub-steps).
- Common-wrong-output in every run-and-see (absent everywhere).
- Checkpoints (present only in sub-step 1) and per-sub-step commits (only one commit, after sub-step 5).
- Closing sequence diagram of the flow built (build ends with Play With It / Finished Result, no sequence diagram).
- Analogy in the Big Idea (diagram present, analogy absent).
- Inline flashcards: recap item (g) only points at `docs/flashcards.md`; the 3-5 cards are not in the lesson.
- Session plan with named sittings and save points for a ~10-hour step.
- Time-boxes per movement and per sub-step; re-entry lines at checkpoints.
- You-are-here / "Sub-step X of 5" progress markers.
- Pasted output for Verification Log items 3 (full-repo `./mvnw verify`) and 5 (clean-room).
- `requests.http` absent from `steps/step-19/` (defensible — the step has no HTTP surface — but worth confirming against the course-wide folder convention).

## Findings

### F1: Build contains zero code — sacred-build contract broken

**Severity:** high
**Lens:** swe
**Location:** C · Let's Build It, sub-steps 1-5 (lines 209-249)
**needsRun:** true
**Issue:** The entire build is prose specs ("`LamportClock` (`tick`, `onReceive`)", "`QuorumSystem` stores a versioned value per replica...") with not a single code block — no pom.xml contents, no Java source, no test code. A nervous beginner at 11pm cannot produce `LamportClock`, `VectorClock`, `QuorumSystem`, `DeliverySim`, `ReplicatedRegister` and 13 tests from method-name hints. The Verification Log even cites "line 25" of a test the reader has never seen. The module exists at `playground/distributed-lab` in the repo, so the lesson is describing real code it never shows.
**Fix:** For each of the 5 sub-steps, inline the complete file(s) from the repo's `playground/distributed-lab` at `step-19-end`: file-path header comment, package line, all imports, full body (use diff view for the parent `pom.xml` `<modules>` edit). Then re-run `./mvnw -pl playground/distributed-lab test -Dtest=<Class>` per sub-step and paste the real output into each run-and-see (do not synthesize output).

### F2: Sub-step micro-anatomy missing across the board

**Severity:** high
**Lens:** pedagogy
**Location:** Sub-steps 1-5 (lines 215-249)
**needsRun:** false
**Issue:** The required per-sub-step sequence (goal → location → code → line-by-line → under-the-hood → predict → run-and-see → checkpoint → commit → pitfall) is almost entirely absent: checkpoint appears only in sub-step 1, pitfall only in sub-step 2, predict only in sub-step 2, and there is a single commit after sub-step 5. No sub-step has a line-by-line explanation or its own under-the-hood.
**Fix:** After F1 lands the code, add to every sub-step: a numbered line-by-line walkthrough of the non-obvious lines, a 2-4 sentence under-the-hood (e.g. sub-step 3: why enumerating subsets is a proof by exhaustion; sub-step 5: why caller-supplied timestamps make LWW deterministic), a one-line checkpoint ("`git status` shows N new files; `-Dtest=X` green"), a `git commit` command per sub-step (split the existing final message into five), and a pitfall each (e.g. sub-step 3: forgetting to bump the version stamp monotonically; sub-step 4: dedupe set keyed on payload instead of message id).

### F3: No session plan, time-boxes, or re-entry support for a ~10-hour step

**Severity:** high
**Lens:** adhd
**Location:** Orient table (line 37) and all movement/sub-step headings
**needsRun:** false
**Issue:** Effort is stated only as a single "≈ 10 hours". There are no planned sittings, no named save points, no per-movement or per-sub-step time-boxes, and no "stopping here? next session starts at..." re-entry lines anywhere.
**Fix:** Add a "Session plan" box at the end of Orient: Sitting 1 (~2h) Understand B; Sitting 2 (~2.5h) sub-steps 1-2, save point = clocks tests green; Sitting 3 (~2.5h) sub-steps 3-4, save point = quorum+delivery green; Sitting 4 (~2-3h) sub-step 5 + Prove + Apply/Review, save point = `step-19-end` tag. Add "⏱️ ~Xh" to each movement heading and "⏱️ ~Xm" to each sub-step heading. At each sub-step checkpoint append: "Stopping here? You have <artifact> working. Next session: Sub-step N, first action: <command>."

### F4: Verification Log claims output it does not paste

**Severity:** high
**Lens:** swe
**Location:** D · Verification Log, items 3-5 (lines 298-302)
**needsRun:** true
**Issue:** The log opens with "All output below is real and pasted", but item 3 (full-repo `./mvnw verify`, 10 modules) says "*(Pasted in the commit's verification run.)*" — i.e., not in the lesson; item 5 (clean-room) is a bare assertion with no output; item 4 shows only a one-line summary of smoke.sh.
**Fix:** Actually run `./mvnw verify` at `step-19-end`, `bash steps/step-19/smoke.sh`, and the clean-room clone, and paste the real tail of each (reactor summary listing the 10 modules with BUILD SUCCESS; smoke.sh's actual final lines). Never describe output in place of pasting it.

### F5: Flashcards are a pointer, not content

**Severity:** medium
**Lens:** structure
**Location:** F · Recap item (g) (line 368)
**needsRun:** false
**Issue:** The contract requires 3-5 flashcards in the recap; the lesson only says they were "appended to `docs/flashcards.md`".
**Fix:** Inline 4 flashcards as Q/A pairs, e.g.: (1) Q: What does CAP actually constrain? A: Behavior during a partition — pick C or A; no partition, no forced choice. (2) Q: PACELC in one line? A: If Partition: A or C; Else: Latency or Consistency. (3) Q: Why does W+R>N give strong reads? A: Every read quorum intersects every write quorum (pigeonhole). (4) Q: What can a vector clock detect that a Lamport clock cannot? A: Concurrency (a || b). Keep the pointer line as a secondary note.

### F6: Build has no closing sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** End of C, between "Play With It" and "The Finished Result" (lines 253-268)
**needsRun:** false
**Issue:** The contract requires the build to close with a sequence diagram of the flow built; the build ends with prose.
**Fix:** Add a Mermaid `sequenceDiagram` of the AP lab's lifecycle: Client->>ReplicaA: write(v=5,t=5); Note over A,B: partition(); Client->>ReplicaB: write(v=9,t=9); Client->>ReplicaA: read → 5 (divergent); heal() → sync (LWW t=9); both reads → 9. Caption it as "the CAP trade-off you just built, end to end."

### F7: Big Idea has diagram but no analogy

**Severity:** medium
**Lens:** pedagogy
**Location:** B · The Big Idea (lines 110-122)
**needsRun:** false
**Issue:** The contract's Understand movement requires big idea + diagram + analogy; the analogy is missing, and for an abstraction-heavy theory step it is the cheapest comprehension aid available.
**Fix:** Add a 3-4 sentence analogy after the Mermaid diagram: two branches of the same bank lose their phone line (partition). Either both branches stop taking withdrawals until the line is back (CP — correct but closed), or both keep serving customers and reconcile the ledgers tonight, accepting a possible overdraft (AP — open but divergent). Even with the line up, waiting for the other branch to confirm every transaction is the latency-vs-consistency "else" of PACELC.

### F8: Wall-of-text in the Understand movement

**Severity:** medium
**Lens:** adhd
**Location:** "Under the Hood: consensus & quorums" (line 158) and the 8-fallacies sentence in the Big Idea (line 112)
**needsRun:** false
**Issue:** The consensus/quorums section is one ~160-word paragraph carrying five distinct ideas (consensus, majority math, fault-tolerance formula, W/R quorums, the consistency dial) with no visual or list break. The 8 fallacies are crammed into a single parenthetical sentence inside an already-dense paragraph.
**Fix:** Convert the consensus/quorums paragraph to 4-5 bullets (one idea each) ending with the `W+R>N` inequality set off as a display line; convert the 8 fallacies into an 8-item bullet list (or a 2-column table) directly under their own bold lead-in.

### F9: Interactivity and progress markers too sparse in the build

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-steps 1-5 (lines 215-249)
**needsRun:** false
**Issue:** The whole build has one predict (sub-step 2) and one break-it (sub-step 3); no knowledge-checks, no type-it-yourself, no you-are-here / "Sub-step X of 5" markers — so there is neither scaffold fading nor visible progress.
**Fix:** Rename headings to "Sub-step N of 5 — ...". Add a predict-then-run to sub-steps 3, 4, 5 (e.g. sub-step 4: "3 deliveries to the naive consumer of a +100 deposit — what balance?"). Add a knowledge-check after sub-steps 2 and 5 (2-3 multiple-choice items on happens-before and CP vs AP). Make sub-step 5's third test (`PACELC else`) type-it-yourself: give the arrange lines, have the learner write the two assertions, with the solution in a `<details>`.

### F10: Course-spec section numbers used without micro-recap

**Severity:** low
**Lens:** adhd
**Location:** Lines 40, 289, 302, 304 ("§12.3 mutation", "§12.4 clean-room", "§12.8 honesty")
**needsRun:** false
**Issue:** §-references to the course-wide verification spec are used as if self-explanatory; a learner mid-step must either remember or leave the lesson to look them up.
**Fix:** At first use of each, add a one-line gloss in parentheses: "§12.3 mutation (course rule: deliberately break the code to prove the test fails, then revert)", "§12.4 clean-room (fresh clone must build green)", "§12.8 honesty (declare what is simulated vs real)".

### F11: Definition of Done is prose; interview prep overshoots the 4-6 band

**Severity:** low
**Lens:** structure
**Location:** "The Finished Result" (line 268); Interview Prep (lines 322-328)
**needsRun:** false
**Issue:** The DoD is a single run-on sentence rather than the contract's checklist, and Interview Prep has 7 Q&A against the contracted 4-6.
**Fix:** Convert the DoD into a 5-item checkbox list (explain the five concepts aloud / `./mvnw verify` green / smoke.sh passes / mutation done and reverted / committed + tagged `step-19-end`). Fold Q7 (the concurrency tie-in) into the Security Lens & Thread-safety note in Movement B, leaving 6 questions.

### F12: Troubleshooting references code the lesson never shows

**Severity:** low
**Lens:** swe
**Location:** Troubleshooting, first bullet (line 344)
**needsRun:** false
**Issue:** The `List.removeLast()` / Java 21+ entry troubleshoots a call that appears in no shown snippet, which reads as noise until F1 inlines the real source.
**Fix:** After F1, either keep the bullet and ensure the shown code containing `removeLast()` is visibly flagged in its line-by-line ("Java 21+ SequencedCollection method"), or delete the bullet if the inlined code does not use it.
