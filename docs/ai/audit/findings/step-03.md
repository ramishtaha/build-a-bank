# Step 3 audit - swe:8 pedagogy:8 adhd:6 structure:9 - thinBuild:false

## Strengths

- Build sub-steps carry the full micro-anatomy (goal, location, complete code with path headers, line-by-line, under-the-hood, predict, run-and-see with real captured output, checkpoint, commit, pitfall) plus break-it-on-purpose drills that contrast a transport-layer failure (Connection refused) with an application-layer failure (404), and every sub-step has a "you are here" breadcrumb.
- The RFC 9110 header-case bug is a genuine narrative thread: seeded as a trick question in sub-step 3's predict, load-bearing in the LoopbackHttpTest comment, and fully unpacked in Troubleshooting with before/after code - excellent "verify, don't guess" modeling.
- The Verification Log states its tier (Standard), pastes platform-honest evidence (Windows schannel caveat vs OpenSSL curl), and the Content-Length: 102 math actually checks out against the UTF-8 multi-byte emoji body.

## Missing spine

- none

## Findings

### F1: No session plan for a 15-20 hour step

**Severity:** high
**Lens:** adhd
**Location:** A - Orient, "This Step in 30 Seconds" table (lines 29-40)
**needsRun:** false
**Issue:** The step announces ~15-20 hours of effort but provides no sitting plan: no 6-10 named sessions of ~2-3h, no named save points, no mapping of sittings to movements/sub-steps. An ADHD learner faces an undifferentiated 20-hour wall with only the whole-step estimate to plan around (the skip-test's "under an hour" is for experts only).
**Fix:** Add a "Session plan" subsection to Orient directly under the 30-seconds table with 6-8 named sittings, e.g.: S1 Orient + Big Idea + mailroom analogy (~2h, save point: none needed); S2 end-to-end story + HTTP anatomy + security lens + then-vs-now (~2.5h); S3 pattern spotlight + build sub-steps 1-2 (~2.5h, save point: commit after sub-step 2); S4 sub-steps 3-4 (~2h, save point: commit after sub-step 4); S5 sub-step 5-6 + Play With It (~2.5h, save point: tag step-03-end); S6 Prove + Apply (~2h); S7 Review + flashcards (~1.5h). Each sitting gets one line: what you'll have working at its end and the first command of the next sitting. While editing, sanity-check the 15-20h claim against this plan and tighten it if it sums lower.

### F2: -q flag contradicts the pasted [INFO] expected output in sub-steps 4 and 5

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 4 Run & See (lines 714-722) and sub-step 5 Run & See (lines 827-835)
**needsRun:** true
**Issue:** Both commands use `./mvnw ... test -q -Dtest=...` but the "Expected output (tail)" shows `[INFO] Tests run: 2 ...` lines. Maven's `-q` suppresses INFO-level logging, so a learner running the exact command sees nothing on success and concludes the expected output is wrong (or that something failed silently). Sub-step 6 correctly omits `-q` and shows [INFO] lines, making 4 and 5 inconsistent with it.
**Fix:** Remove `-q` from the two commands in sub-steps 4 and 5, re-run each exact command, and paste the true tail. Alternatively keep `-q` and paste the real (near-silent) output with a note explaining quiet mode - but dropping `-q` matches sub-step 6 and gives the learner visible confirmation. Do not keep the current mismatched pairing.

### F3: Zero type-it-yourself - no scaffold fading across five fully worked files

**Severity:** medium
**Lens:** pedagogy
**Location:** C - Build, sub-steps 1-5 (lines 357-847)
**needsRun:** false
**Issue:** All five files are complete copy-paste worked examples; the scaffolding never fades. The Orient section even promises the learner will have "typed an HTTP request byte-by-byte over a raw socket," but sub-step 3 hands them the finished request string. The contract's interactivity toolkit includes type-it-yourself and the pedagogy rubric expects later sub-steps to shift away from fully worked code.
**Fix:** (1) In sub-step 3, present RawHttpDemo with the four request-string lines blanked (`String request = /* TYPE THE FOUR HTTP LINES YOURSELF: request line, Host, Accept, Connection: close, then the blank line - CRLF endings! */;`) and put the completed string in a collapsed `<details>` reference block. (2) In sub-step 4, give the first test method worked and ask the learner to write `usesExplicitPortAndHttpDefault` themselves from the sub-step 1 predictions, with the solution in a `<details>` block. No new outputs needed - the final code is unchanged.

### F4: Checkpoints lack re-entry lines for resuming a session

**Severity:** medium
**Lens:** adhd
**Location:** Checkpoint lines in sub-steps 1-6 (lines 421, 528, 650, 724, 837, 871)
**needsRun:** false
**Issue:** Each checkpoint verifies state but none supports stopping and re-entering: no "stopping here? you have X working; next session starts at sub-step N, first action: ..." A learner returning after two days must re-read the build to find their place.
**Fix:** Append one standardized line to each of the six checkpoints, e.g. after sub-step 2's checkpoint: "Stopping here? You have UrlAnatomy + a working JDK-client round trip committed. Next session: sub-step 3 (raw socket) - first action: `make run-hello` in terminal 1, then reopen this page at 'Sub-step 3 of 6'." Repeat with adjusted content for sub-steps 1, 3, 4, 5; sub-step 6's line points at Play With It / the Verification Log.

### F5: No time-boxes per movement or per sub-step

**Severity:** medium
**Lens:** adhd
**Location:** "The Six Movements of This Step" map (lines 12-22) and each sub-step heading (lines 357, 434, 541, 663, 737, 850)
**needsRun:** false
**Issue:** The only time signals are the whole-step 15-20h, a "5-minute self-check," and one "60s" break-it. There is no per-movement or per-sub-step estimate, so a learner cannot budget a sitting or know whether being 40 minutes into sub-step 3 is normal or a sign they're stuck.
**Fix:** Add an estimate to each entry in the six-movements map (e.g. "B - Understand (~4-5h)") and to each sub-step heading (e.g. "Sub-step 3 of 6 (~30-40 min) - Speak HTTP the raw way"). Keep estimates consistent with the F1 session plan.

### F6: Sub-step 1's run-and-see is a silent success - no visible first win

**Severity:** medium
**Lens:** adhd
**Location:** Sub-step 1 Run & See (lines 413-419)
**needsRun:** true
**Issue:** The first build action ends with `test-compile -q` whose expected output is literally nothing ("a silent success"). The first visible result comes only in sub-step 2, after also starting the hello-service. For first-win-fast, the learner should see something real within minutes of starting to build.
**Fix:** After the compile in sub-step 1, add a short "see it live" block: run `jshell --class-path playground/java-basics/target/classes`, then `UrlAnatomy.of("https://api.bank.example/accounts?limit=10")`, and paste the record's actual toString output (which also pre-answers the sub-step's predict). Capture the real jshell output by running it - do not fabricate it.

### F7: RawHttpDemo sends Host without the non-default port

**Severity:** low
**Lens:** swe
**Location:** Sub-step 3 code, the request string (lines 573-577), and its line-by-line bullet (line 602)
**needsRun:** true
**Issue:** The hand-built request sends `Host: localhost` while connecting to port 8080. RFC 9112 says the Host header carries `uri-host [":" port]` and the port is included when it isn't the scheme default - the lesson's own curl trace (line 1010) correctly shows `Host: localhost:8080`. Tomcat tolerates it, but a lesson teaching exact wire anatomy should model the correct form and the discrepancy with the curl capture is visible to an attentive learner.
**Fix:** Change the request line to `"Host: " + host + ":" + port + "\r\n"` in RawHttpDemo, update the line-by-line bullet to state the RFC rule (port included when non-default; compare the curl -v trace), re-run the demo and the loopback test to confirm output/tests are unchanged, and adjust the step-03-end reference code to match.

### F8: Prose example IP contradicts the step's own observed DNS output

**Severity:** low
**Lens:** swe
**Location:** B - Understand, end-to-end story item 1 (line 169) and cheat-card diagram (line 86); Verification Log item 5 (lines 1021-1031)
**needsRun:** false
**Issue:** The prose says DNS returns "93.184.x.x" for example.com - the pre-2025 textbook address - while the step's own pasted nslookup shows a 2606:4700:... address. A lesson whose refrain is "verify, don't guess" should not present a stale example that its own Verification Log contradicts.
**Fix:** In line 169 and the line-86 diagram, replace "93.184.x.x / 2606:..." with wording anchored to observation, e.g. "an IPv4 and/or IPv6 address - on this machine nslookup returned 2606:...; yours will differ (example.com has changed hosts over the years - another reason to observe rather than memorize IPs)."

### F9: Optional content has no time-cost labels

**Severity:** low
**Lens:** adhd
**Location:** E - Apply: Go Deeper details blocks (lines 1061-1099) and Stretch exercises (lines 1147-1153); Play With It experiments (lines 937-942)
**needsRun:** false
**Issue:** Optional deep-dives and stretch exercises are marked optional but carry no time cost, so a learner cannot decide whether to engage now or defer - a known overwhelm/decision-fatigue trap.
**Fix:** Add a parenthetical estimate to each: the three Go Deeper summaries ("~10 min read"), each stretch exercise ("~30 min", "~30 min", "~1-2h" for ContentLengthAwareRawHttp), and the four Play With It experiments ("~2 min each").

### F10: Knowledge-checks appear only once in the build

**Severity:** low
**Lens:** pedagogy
**Location:** C - Build; the only knowledge-check is in sub-step 3 (line 648)
**needsRun:** false
**Issue:** The interactivity toolkit expects knowledge-checks sprinkled through the build; only sub-step 3 has one. Predicts exist everywhere, but there is no retrieval moment after sub-steps 2, 4, or 5 while concepts (negotiated version, ephemeral ports) are fresh.
**Fix:** Add two more hidden-answer knowledge-checks: after sub-step 2's run ("The output said HTTP_1_1 - where did that value come from: the request you built, or the exchange itself? Why does 'negotiated' matter?") and after sub-step 5's run ("Why does the test ask for port 0 and then read the port back, instead of hard-coding 18080?"), each with a 2-3 sentence `<details>` answer drawn from existing lesson text.
