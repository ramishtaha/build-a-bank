# Step 14 audit - swe:6 pedagogy:7 adhd:4 structure:8 - thinBuild:true

## Strengths

- Technically honest treatment of hard topics: the dual-write gap is flagged with a forward reference to the Outbox (Step 20) instead of pretending direct send is complete; constant-time compare, replay windows, and the Boot 4 Jackson 3 gotcha are all called out with real reasoning.
- Strong Orient/Review bookends: measurable outcomes align cleanly with the Definition of Done, test-yourself, and flashcards; prior-step callbacks (Steps 10-13) are explicit and each says what carries forward.
- You-are-here markers on every sub-step, plus a Pattern Spotlight (idempotency key) with genuine alternatives/trade-off discussion (natural key, request hash, TTL).

## Missing spine

- run-and-see (exact command + expected output + common-wrong-output) missing in sub-steps 0, 1, 2; sub-step 5's is labeled optional with no expected output; only sub-step 6 has an output block
- complete code with file-path header comments, package lines, and imports missing in every sub-step; no diff view for the TransferController edit (an edit to an existing file)
- whole files never shown at all: `IdempotencyRecordRepository`, `LedgerEntryResponse`, `TransferService.entriesOf(...)`, the full `WebhookSender` class (fields, `MAX_ATTEMPTS`, `sleepBackoff`), the full `WebhookPublisher` (config binding for `bank.webhook.url`/`secret`), the application.yaml additions, and the full source of all three new test classes
- common-wrong-output absent everywhere
- predict-then-run present only in sub-steps 1 and 3; type-it-yourself absent from the entire build; break-it-on-purpose appears once (sub-step 6)
- no session plan / per-movement or per-sub-step time-boxes for a ~20-hour step; no re-entry lines at checkpoints

## Findings

### F1: Build code is fragments — a beginner cannot complete this build unaided
**Severity:** high
**Lens:** swe
**Location:** C · Build, all sub-steps 0-6 (lines 237-531)
**needsRun:** false
**Issue:** Every code block is an excerpt ("the key pieces", "the heart", `// ctor injection`, `// + no-arg ctor, all-args ctor, getters`) with no package lines, no imports, no file-path header comments. Several snippets cannot compile as shown even as excerpts: `Mac.getInstance`/`mac.init` throw checked `NoSuchAlgorithmException`/`InvalidKeyException` with no throws/try (sub-step 3); `objectMapper.writeValueAsString` throws checked `JsonProcessingException` (sub-step 4); `toHex` is called but never defined. Entire files a learner must produce are never shown: `IdempotencyRecordRepository`, `LedgerEntryResponse`, `TransferService.entriesOf(...)`, the full `WebhookSender` (HttpClient field, `MAX_ATTEMPTS`, `sleepBackoff`), the full `WebhookPublisher` (how `url`/`secret` are injected, the `bank.webhook.url`/`bank.webhook.secret` config keys), and all three test classes. This violates the sacred-build contract for every sub-step.
**Fix:** For each sub-step, replace the excerpt with the complete file(s) from the repo at tag `step-14-end` (which is already built and verified): file-path header comment, package line, all imports, full class body. Show `TransferController` as a diff view since it edits an existing file. Add the application.yaml/config snippet for `bank.webhook.url`/`bank.webhook.secret` to sub-step 4. Keep the existing line-by-line commentary and extend it to the newly shown members (`toHex`, `sleepBackoff`, config binding).

### F2: ~20-hour step with no session plan, no time-boxes, no re-entry support
**Severity:** high
**Lens:** adhd
**Location:** A · Orient "This Step in 30 Seconds" (line 35) and all Build checkpoints
**needsRun:** false
**Issue:** The step declares "≈ 20 hours focused" but provides zero session structure: no planned sittings, no named save points, no per-movement or per-sub-step time estimates, and no re-entry lines at checkpoints. An ADHD learner has no way to slice this into 2-3h sessions or to resume after stopping mid-build.
**Fix:** Add a "Session plan" box at the end of Orient splitting the step into 7-8 sittings of ~2.5-3h, each named and mapped to content (e.g., S1: Orient+Understand; S2: sub-steps 0-1; S3: sub-step 2 + play; S4: sub-step 3; S5: sub-step 4; S6: sub-steps 5; S7: sub-step 6 + Prove; S8: Apply+Review), each ending at an existing commit. Add a time-box to each movement heading (e.g., "B · Understand — ~2.5h") and each sub-step heading (e.g., "Sub-step 3 — ~2h"). At each sub-step's ✋ Checkpoint, append a re-entry line: "Stopping here? You have <X> working and committed; next session starts at sub-step <N>, first action: <command/file>."

### F3: Run-and-see with expected output missing from most sub-steps
**Severity:** high
**Lens:** swe
**Location:** Sub-steps 0 (line 267), 1 (line 309), 2 (line 341), 5 (lines 469-475)
**needsRun:** true
**Issue:** Sub-steps 0-2 end with checkpoints of "compiles" — no ▶️ Run & See block, no expected output, no common-wrong-output. Sub-step 5's Run & See is marked "optional" and shows commands with a prose comment instead of expected output. Only sub-step 6 has an expected-output block. The contract requires run-and-see (exact command + expected output + common-wrong-output) for every sub-step, and the reward loop is nearly absent for the first half of the build.
**Fix:** For each of sub-steps 0, 1, 2, and 5, actually run the stated command at that point in the build and paste the real output: sub-step 0 — run the app or a test against Testcontainers and paste the Flyway "Successfully applied 2 migrations" lines; sub-steps 1-2 — run the compile plus the relevant test slice and paste the tail; sub-step 5 — run the compose+spring-boot:run sequence and paste the curl responses (idempotent retry, PageResponse body, Deprecation headers). Add one common-wrong-output per sub-step (e.g., Flyway checksum mismatch, `ddl-auto=validate` mapping failure, missing mock in the WebMvcTest slice).

### F4: Sub-steps 3-4 run and commit tests that are not written until sub-step 6
**Severity:** high
**Lens:** swe
**Location:** Sub-step 3 checkpoint + commit (lines 376-378), sub-step 4 checkpoint (line 423), vs sub-step 6 (lines 485-489)
**needsRun:** false
**Issue:** Sub-step 3's checkpoint runs `-Dtest=WebhookSignerTest` and its commit `git add`s `WebhookSignerTest.java`; sub-step 4's checkpoint runs `WebhookDeliveryTest`. But those test classes are only introduced (as fragments) in sub-step 6. A learner following top-to-bottom hits "No tests matching pattern" (or a git add of a nonexistent file) at sub-step 3 and cannot pass the checkpoint.
**Fix:** Move the full source of `WebhookSignerTest` into sub-step 3 and `WebhookDeliveryTest` into sub-step 4 (write-the-test-then-run within the same sub-step), leaving sub-step 6 to cover `IdempotencyTest` plus the updates to `TransferControllerTest` and `DemandAccountIntegrationTest`. Update sub-step 6's Goal/Location lines and the files-we'll-touch tree accordingly.

### F5: Verification Log promises "real pasted output" but most sections are prose summaries
**Severity:** medium
**Lens:** structure
**Location:** D · Prove, sections 1-4 and 7 (lines 586-623)
**needsRun:** true
**Issue:** The log's header claims "Real pasted output below", but §1 shows only a two-line tail (the per-class counts are prose), §§2-4 are bullet summaries of what tests proved with no console output at all, and §7 (clean-room) is one prose line. Only §5 (mutation) and §6 (smoke) contain pasted output. As written, the evidence for the step's headline claims (idempotent retry, signed delivery, replay rejection over real HTTP) is asserted, not shown.
**Fix:** Re-run the verification suite and paste real excerpts: the surefire per-class summary lines for §1, the relevant test-method output/assertions log for §§2-4 (or the failsafe/surefire lines naming each test method green), and the tail of the clean-room `./mvnw verify` for §7.

### F6: Deprecation header mis-attributed to RFC 8594 and uses the obsolete draft syntax
**Severity:** medium
**Lens:** swe
**Location:** B · Under the Hood "Versioning & deprecation headers" (line 160), Then-vs-Now (line 187), verify-don't-guess note (line 190), resources (line 694), sub-step 5 code (line 443)
**needsRun:** false
**Issue:** RFC 8594 defines only the `Sunset` header. The `Deprecation` header is standardized by RFC 9745 (2025), where its value is a structured-field date (`Deprecation: @1761955199`), not `Deprecation: true` — that boolean form is the expired draft syntax. The lesson repeatedly cites "RFC 8594 deprecation headers" and teaches the legacy value.
**Fix:** In prose and resources, attribute `Sunset` to RFC 8594 and `Deprecation` to RFC 9745, and note the standardized value is an `@<unix-timestamp>` structured date while `Deprecation: true` is the widely-seen legacy draft form (state explicitly which one the code uses and why). If the code is updated to the RFC 9745 format, re-run the affected tests before pasting new output.

### F7: Sub-step 2's commit command omits half the files the sub-step changes
**Severity:** medium
**Lens:** swe
**Location:** Sub-step 2 commit (line 343)
**needsRun:** false
**Issue:** The sub-step's stated location includes `LedgerEntryRepository` (new paged finder) and `TransferService.entriesOf(...)`, but the commit only `git add`s `web/PageResponse.java` and `web/LedgerEntryResponse.java`. A learner following the commands commits a broken slice and carries uncommitted changes into the next sub-step's history.
**Fix:** Change the commit to `git add` all four paths: `domain/LedgerEntryRepository.java`, `service/TransferService.java`, `web/PageResponse.java`, `web/LedgerEntryResponse.java` (adjust to actual repo paths).

### F8: "Under the Hood" is a ~900-word wall of six dense paragraphs
**Severity:** medium
**Lens:** adhd
**Location:** B · Under the Hood (lines 158-170)
**needsRun:** false
**Issue:** Six back-to-back paragraphs of 120-160 words each (versioning, idempotency-under-concurrency, pagination, HMAC, at-least-once, Jackson gotcha) with no code block, diagram, or interactive break between them — the single worst wall-of-text in the lesson. The Security Lens that follows is another unbroken 5-item dense list, compounding the load.
**Fix:** Break Under the Hood into per-topic `####` mini-headings; after each, insert a tiny concrete artifact: a 3-line HTTP block showing the three deprecation headers, a 2-line sequence of the racing-duplicates outcome, a one-line `signature = HMAC_SHA256(secret, ts + "." + body)` formula block, and a one-question knowledge-check (`<details>`) between the webhook paragraphs and the Jackson gotcha.

### F9: Interactivity is sparse and scaffolding never fades
**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, sub-steps 0, 2, 4, 5, 6 (predicts only at lines 307 and 374); B · Understand (no knowledge-checks)
**needsRun:** false
**Issue:** Only 2 of 7 sub-steps have a predict-then-run; there is no type-it-yourself anywhere, one break-it (sub-step 6), and no knowledge-checks in Understand. The contract expects the toolkit sprinkled throughout and later sub-steps shifting toward type-it-yourself; here every sub-step is presented the same way (a worked fragment), so there is no scaffold fading.
**Fix:** Add predicts to sub-steps 0 ("what does Flyway report on next startup?"), 2 ("what SQL does `findByAccountId(…, Pageable)` generate?"), 4 ("receiver returns 500 once — how many attempts?"), and 5 ("what headers does the old endpoint return?"). Convert one late component to type-it-yourself with a hidden solution — e.g., in sub-step 5 give the method signatures for `transferV1`/`entries` and have the learner write the bodies from the earlier pieces, with the full code in a `<details>`. Add two knowledge-checks in Understand (one after idempotency, one after webhooks).

### F10: What the losing concurrent request actually receives is never addressed
**Severity:** medium
**Lens:** swe
**Location:** Pattern Spotlight "How it works" (line 152) and Under the Hood "Idempotency under concurrency" (line 162)
**needsRun:** false
**Issue:** The lesson correctly explains that of two racing duplicates only one commits and the other rolls back on the unique violation — but never says what that loser's client sees. As coded, the losing request surfaces a `DataIntegrityViolationException` → HTTP 500, not the stored `transactionId`, so the client must retry a third time to get the idempotent hit. Presenting the guard without this caveat overstates the guarantee ("a retry returns the stored result") for the concurrent case.
**Fix:** Add 2-3 sentences to Under the Hood (and one line to interview Q6): the loser's transaction rolls back and, in this implementation, returns an error; a subsequent retry then gets the stored result. Note the production hardening: catch the unique-violation in `IdempotentTransferService`, re-read the key in a new transaction, and return the stored `transactionId` (or a 409), as in Go Deeper ②.

### F11: First visible win comes too late in the build
**Severity:** low
**Lens:** adhd
**Location:** C · Build, sub-steps 0-2 (lines 237-346)
**needsRun:** true
**Issue:** The first thing the learner *sees run* beyond "compiles" is the WebhookSignerTest at sub-step 3 — realistically 4-6 hours in. Sub-steps 0-2 end with compile-only checkpoints, giving no visible result in the first ~10 minutes of the build.
**Fix:** After sub-step 0, add a 5-minute observable win: start the app against the compose Postgres (or run any Testcontainers test) and paste the real Flyway log lines showing `Migrating schema ... to version "2 - idempotency keys"` / `Successfully applied 2 migrations` so the learner sees the new table land before writing any service code. (Overlaps with F3; do together.)

### F12: Sub-step numbering "0 of 6" through "6 of 6" is seven sub-steps out of six
**Severity:** low
**Lens:** structure
**Location:** Build sub-step headings (lines 237, 275, 317, 349, 384, 431, 485) and the flow diagram (lines 213-221)
**needsRun:** false
**Issue:** The build has seven sub-steps labeled "Sub-step 0 of 6" … "Sub-step 6 of 6" — the "of 6" reads as a total but is actually the max index, so the learner's progress denominator is wrong (at "3 of 6" they are past the halfway point in count but appear to be at half).
**Fix:** Renumber to "Sub-step 1 of 7" … "Sub-step 7 of 7" in every heading, in the you-are-here markers, in the build-overview flowchart node labels, and in any cross-references (e.g., "the entries endpoint (sub-step 5)" → "sub-step 6").
