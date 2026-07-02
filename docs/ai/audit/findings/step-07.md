# Step 7 audit - swe:9 pedagogy:8 adhd:6 structure:9 - thinBuild:false

## Strengths

- The step's core idea (self-invocation pitfall) is proven, not asserted: a counter-based test, a deliberate break-it inversion (`isEqualTo(2)` must fail), and runtime CGLIB inspection make the proxy boundary observable at every layer.
- Honest Boot-4 version narrative: the two real build failures (starter-aop removed, `spring-boot-webmvc-test` split) are staged on purpose so the learner sees, recognizes, and fixes the actual errors instead of copying working config blindly.
- Complete micro-anatomy with dense reward loops: every sub-step ends in a run, all code blocks carry file-path headers and full imports, and the build is bracketed by a what-we-will-build diagram, files tree, and a closing sequence diagram.

## Missing spine

- none (all six movements, cheat card, skip-test, Depends-on line, DoD, stated verification tier, 6 interview Q&A, 5 test-yourself items, 5 flashcards, and resolving TOC anchors are present)

## Findings

### F1: No session plan for a ~20-hour step

**Severity:** high
**Lens:** adhd
**Location:** A - Orient, "This Step in 30 Seconds" table (lines 29-41); applies to the whole lesson
**needsRun:** false
**Issue:** The step is billed at ~20 hours of focused effort, but there is no planned-sittings structure anywhere: no list of 6-10 sessions of ~2-3h, no named save points, and no re-entry lines at checkpoints ("stopping here? you have X working; next session starts at sub-step N, first action: ..."). An ADHD learner has to self-segment 20 hours across 7 sub-steps plus Understand/Apply/Review with no scaffolding, and every checkpoint assumes a continuous run.
**Fix:** Add a "🗓️ Session Plan" subsection at the end of Orient with ~7 sittings, each ~2.5-3h, each ending at an existing commit point, e.g.: S1 = Orient + Understand (read only, no code); S2 = sub-steps 1-2 (save point: commit "aspect compiles"); S3 = sub-step 3 (save point: slice compiles); S4 = sub-steps 4-5 (save point: proxy seen + curl works); S5 = sub-steps 6-7 (save point: 16 tests green); S6 = Play With It + Prove + smoke.sh; S7 = Apply + Review + Capstone challenge. Then append one re-entry line to each sub-step's ✋ Checkpoint: "Stopping here? You have <artifact> working and committed. Next session: sub-step N+1, first action: <exact command or file to open>."

### F2: No time-boxes on movements or sub-steps

**Severity:** medium
**Lens:** adhd
**Location:** Movement headings (lines 27, 126, 238, 1172, 1227, 1325) and sub-step headers (lines 300, 401, 574, 772, 854, 896, 998)
**needsRun:** false
**Issue:** Only the whole-step effort (~20h) and one "60s" break-it are time-boxed. There is no per-movement or per-sub-step estimate, so a learner cannot decide "do I have time to start sub-step 3 tonight?" — the exact decision the 20h figure forces nightly.
**Fix:** Append an estimate to each movement heading (e.g., "B - 🧠 Understand *(~2h)*") and to each sub-step header (e.g., "Sub-step 3 of 7 ... *(⏱ ~90 min)*"). Suggested splits: Orient 30min; Understand 2h; sub-steps 1: 45min, 2: 90min, 3: 2h, 4: 45min, 5: 45min, 6: 90min, 7: 90min; Play With It 45min; Prove 30min; Apply 2h; Review + Capstone 2-3h.

### F3: Sub-step 3 overloads working memory — four files and the course's first web layer in one gulp

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-step 3 of 7 (lines 574-768)
**needsRun:** false
**Issue:** Sub-step 3 introduces four new files and roughly ten first-appearance tokens (`@Repository`, `ConcurrentHashMap`/`ConcurrentMap`, `@Service`, `@RestController`, `@RequestMapping`, `@GetMapping`, `@PathVariable`, `ResponseEntity`, Jackson serialization, `DispatcherServlet`) — and this is the learner's first-ever Spring MVC code in the course (the lab was non-web through Step 6). One compile check at the very end is the only feedback for all four files. This far exceeds the ~3-new-terms-per-sub-step budget and is the lesson's single riskiest abandonment point.
**Fix:** Split into "Sub-step 3a - the data half: `Account` + `InMemoryAccountStore`" (ends with the same `./mvnw -pl playground/spring-lab -am compile` run-and-see, its own checkpoint and commit) and "Sub-step 3b - the web half: `AccountService` + `AccountController`" (keeps the existing service/controller line-by-lines, under-the-hood, predict, run-and-see, commit). Renumber to "of 8" and update the you-are-here trails and the files-tree callout accordingly.

### F4: No scaffold fading — the last sub-steps are still fully worked copy-paste

**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-steps 6 and 7 (lines 896-1091)
**needsRun:** false
**Issue:** All seven sub-steps present complete, final code to transcribe. By sub-step 7 the learner has seen `@SpringBootTest`, `@Autowired`, AssertJ, and the counter API, yet the keystone test is still handed over whole — the contract expects later sub-steps to shift toward type-it-yourself so the learner retrieves rather than copies.
**Fix:** In sub-step 7, convert the code block to a scaffold: give the class skeleton, the three test-method names, and per-method hint comments ("reset the counter; call summaryFor(\"1\") through the injected bean; assert total() — 1 or 2? — and assert calls() contains no findById"), and move the full working listing into a collapsed `<details><summary>Stuck? Full listing</summary>` fallback. Optionally do the same for the third test method of sub-step 6 (`listsAllAccounts`).

### F5: Sub-step 5 "expected output" does not match what `curl -i` actually prints

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 5, ▶️ Run & See (lines 862-875); same summarized format reused in Prove section 3 (lines 1196-1201)
**needsRun:** true
**Issue:** The command is `curl -i http://localhost:8080/api/accounts/1`, but the ✅ expected block shows a summarized line (`GET /api/accounts/1   -> HTTP 200  {...}`) that curl never prints — real `curl -i` output starts with `HTTP/1.1 200`, then headers (`Content-Type: application/json`, ...), then the JSON body. A nervous learner comparing screen-to-lesson will conclude something is wrong. The Prove section labels the same summarized lines "real, pasted output," where they are only truthful as smoke.sh's own echo format.
**Fix:** Start the app, run both curls, and paste the genuine `curl -i` output into sub-step 5 (status line + 2-3 key headers + body; elide the rest with `...`). In Prove section 3, either label the block explicitly as "smoke.sh summary lines" or replace it with the same real curl transcript.

### F6: The "headline experiment" in sub-step 5 cannot actually be run

**Severity:** medium
**Lens:** swe
**Location:** Sub-step 5, 🔬 Break-it (line 884)
**needsRun:** true
**Issue:** The break-it labeled "the headline experiment (60s)" tells the learner to hit the `summaryFor` self-invocation path, then admits "There's no direct endpoint for it, so do it from a quick test or just reason it through." The step's central phenomenon — one AUDIT pair, no inner `findById` line — is never observable live in the app log; it is deferred entirely to sub-step 7's test. That is a broken interactivity promise at the exact moment of peak curiosity.
**Fix:** Add a small temporary (or permanent) `@GetMapping("/{id}/summary")` handler in `AccountController` returning `service.summaryFor(id)`, have the learner curl `/api/accounts/1/summary`, and paste the real app-log excerpt showing exactly one `AUDIT ▶/✔` pair for `summaryFor` and no `findById` line. If kept permanent, note it in the files tree and adjust the MockMvc test count claims; if temporary, instruct removal before the sub-step 5 checkpoint.

### F7: Dangling references to an external spec ("§12 mindset", "§8.1 gold-standard example")

**Severity:** medium
**Lens:** adhd
**Location:** Lines 228 ("the §12 mindset in miniature"), 741 ("the §8.1 gold-standard example"), 1076 ("the §12 mindset")
**needsRun:** false
**Issue:** Three places cite section numbers of a document the learner does not have open (the course-authoring spec). Jargon-before-definition plus a distant cross-reference with no micro-recap: the learner must either shrug past it or go hunting, both of which break flow.
**Fix:** Replace each with self-contained wording: line 228/1076 -> "the course's verify-don't-guess discipline: prove claims by running them, and deliberately break a test once to prove it can fail"; line 741 -> "the same Optional-to-404 idiom used throughout this course's controllers." If the spec is a real in-repo doc, link it by path instead of bare section numbers.

### F8: pom.xml edit shown as full-section replacement, not a diff

**Severity:** low
**Lens:** structure
**Location:** Sub-step 1, ⌨️ Code block (lines 329-365)
**needsRun:** false
**Issue:** The contract requires diff view for edits to existing files. Sub-step 1 replaces the whole `<dependencies>` section wholesale; a learner whose Step-6 pom drifted (or who half-applied the temporary starter-aop block) can clobber existing entries and cannot see at a glance which three dependencies are actually new.
**Fix:** Present the change as a diff: context lines for the existing `spring-boot-starter` and `spring-boot-starter-test` entries, `+` lines for `spring-boot-starter-web`, `org.aspectj:aspectjweaver`, and `spring-boot-webmvc-test` (with their comments). Keep the full final section available in a collapsed `<details>` for verification.

### F9: Optional content carries no time cost

**Severity:** low
**Lens:** adhd
**Location:** 🚀 Go Deeper (lines 1229-1267), 🎮 Play With It experiments table (lines 1137-1148), 🏅 Capstone challenge (lines 1422-1428)
**needsRun:** false
**Issue:** The three Go-Deeper `<details>` blocks, the five Play-With-It experiments, and the capstone POST challenge are optional but unlabeled for time, so a learner cannot budget them — the exact trap that turns a 20h step into 30h for completionists.
**Fix:** Add inline estimates: each Go-Deeper summary "(~10 min read)"; the experiments table header or each row "(~5 min each; ~25 min for all five)"; the capstone challenge "(~60-90 min)". State explicitly that all are skippable without affecting step-07-end.

### F10: Minor micro-anatomy gaps in sub-steps 2, 3, and 5

**Severity:** low
**Lens:** structure
**Location:** Sub-step 2 (lines 549-561), sub-step 3 (lines 747-759), sub-step 5 (lines 854-893)
**needsRun:** false
**Issue:** Sub-steps 2 and 3 have run-and-see blocks but no ❌ common-wrong-output (sub-steps 1, 4, and 6 all have one); sub-step 5 has no 💭 under-the-hood element. Small holes in an otherwise complete per-sub-step anatomy.
**Fix:** Add to sub-step 2 a ❌ block: "`cannot find symbol: class Aspect` -> the `aspectjweaver` dependency is missing (re-do sub-step 1)"; to sub-step 3: "`package org.springframework.web.bind.annotation does not exist` -> `spring-boot-starter-web` missing." Add a one-paragraph 💭 under-the-hood to sub-step 5: curl opens a TCP socket to embedded Tomcat, a worker thread runs the DispatcherServlet pipeline into the proxied service — the same path MockMvc will later simulate in-process.
