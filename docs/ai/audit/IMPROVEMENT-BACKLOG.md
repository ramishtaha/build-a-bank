# Improvement Backlog — Verified Re-enrichment Work Orders (Steps 1–30)

> **What this file is.** A ready-made instruction set for future **verified re-enrichment** sessions. It contains every audit finding that **cannot be fixed by pure editing**: all `needsRun:true` findings, plus a full re-enrichment order for every `thinBuild:true` lesson (the repo NEVER invents command output — thin builds must be re-filled with code transcribed from the step's tag and output captured from real runs).
>
> **How to use it.** Each session: pick **one step** (top-down — the list is ordered by severity × learner impact, thin builds on money/security/concurrency steps first). Then:
> 1. `git checkout -b enrich/step-NN step-NN-end` (use `step-NN-start` + replay when intermediate-state output is needed).
> 2. Re-run the commands listed in the work orders; capture output verbatim (trim with explicit `...` markers only).
> 3. Enrich `steps/step-NN/lesson.md` with the real code (file-path header comment, package line, all imports; diff view for edits) and the real output, following **`docs/ai/LESSON-SPEC.md` §2** (sub-step micro-anatomy).
> 4. Never paste output you did not produce; never reorder or annotate inside a fenced output block.
>
> **Scope key:** S ≈ ≤1h (a few command captures) · M ≈ 2–4h (multiple runs / some intermediate states) · L ≈ full session(s) (whole-build transcription + per-sub-step runs).
>
> Pure-edit findings (session plans, time-boxes, flashcards inline, analogies, diagrams, renumbering, prose fixes) are **not** listed here — see `AUDIT-REPORT.md` themes T2/T5/T9–T15 and the per-step findings files.

---

## Tier 1 — thin builds on money / security / concurrency paths

### Step 12 — Transactions & the money ledger (thinBuild) — capstone money path
- **WO-12.1 (F1, F2, F5) Rebuild the build movement with real code + per-sub-step runs.** Missing: most files never shown ("full file in the repo"), no run-and-see for sub-steps 0–4, first win ~8–12h in. Re-run at `step-12-end` (transcribe code) and on a replay branch from `step-12-start` for intermediate states: `./mvnw -q -pl services/demand-account -am compile` per sub-step; `./mvnw -pl services/demand-account test -Dtest=TransferServiceTest` and `-Dtest=TransactionPropagationTest`; first-win: `docker compose -f services/demand-account/compose.yaml up -d` + boot + `curl localhost:8082/actuator/flyway` (paste JSON). Add one induced common-wrong-output per block (Flyway checksum error, Docker-not-running error). **Scope: L**
- **WO-12.2 (F9) Verification Log evidence.** Missing: §2/§5/§6/§8 are prose. Re-run at `step-12-end`: the named test classes (surefire per-class lines), the live HTTP round-trip curls, and clean-room `git clone` → `./mvnw verify` (paste reactor tail). **Scope: M**
- **WO-12.3 (F7) Reconcile sub-step 6 expected output.** Shown test prints nothing; expected output shows `[capstone:...]` lines. Run the capstone test at `step-12-end`; paste real output and sync the shown code (include its print statements). **Scope: S**

### Step 21 — Payment saga + Redis idempotency + DLT (thinBuild) — money path
- **WO-21.1 (F1, F9) Full build re-enrichment.** Missing: zero code in 5 sub-steps; nothing runs until Play With It. At `step-21-end`, transcribe `PaymentStepService`, `PaymentService`, `RedisIdempotencyStore`, controller/DTOs/422 mapping, Redis config + pom, `KafkaErrorHandlingConfig`, and the three tests (`PaymentSagaTest`, `PaymentControllerTest`, `DeadLetterTest`). Re-run per sub-step: `./mvnw -pl services/... test -Dtest=<Class>` (paste each tail); first win: `docker run -d --name bank-redis -p 6379:6379 redis:7.4-alpine` + `redis-cli SET pay:demo 123 NX EX 60` twice (paste `OK` then `(nil)`). **Scope: L**

### Step 16 — JWT auth service & Spring Security (thinBuild) — security
- **WO-16.1 (F1, F3 context, F4) Build re-enrichment + visible reward loop.** Missing: all 5 sub-steps are fragments; `application.yml`/`AuthApplication`/`AuthDtos`/requests.http/smoke.sh never built; nothing runs before sub-step 3. At `step-16-end`, transcribe the full files; re-run: `./mvnw -q -pl services/auth dependency:resolve` (tail); boot empty app + `curl -i localhost:8083/api/auth/me` (paste the default 401); after config: `curl /actuator/health` 200 vs `/api/auth/me` 401; BCrypt double-hash demo (JShell or temp test, paste both `$2a$10$...` values). **Scope: L**
- **WO-16.2 (F10) Verify `make run-auth`.** Missing: instructed target never proven. At `step-16-end` run `make run-auth`; if absent, replace both mentions with `./mvnw -pl services/auth spring-boot:run`. **Scope: S**

### Step 17 — RS256/JWKS cross-service auth (thinBuild) — security
- **WO-17.1 (F1, F2, F6) Build re-enrichment + missing test code + runnable middle.** Missing: fragments everywhere (`rolesConverter()` used, never defined); checkpoints demand 11/31 green tests never shown; sub-steps 2–4 have no runnable run-and-see. At `step-17-end`, transcribe both `SecurityConfig.java` files, `JwksController`, the `JwtService` diff, the mint helper and new test methods. Re-run: `curl /api/auth/admin-method` with USER (403) and ADMIN (200) tokens; `curl -i localhost:8082/api/accounts` no-token (paste 401 + `WWW-Authenticate: Bearer`); compile tails for sub-step 3. **Scope: L**
- **WO-17.2 (F10) Verification Log §2 vs cheat card.** Missing: §2 is a hand-formatted summary contradicting the cheat card's data (200.00 vs 100.00). Re-run the live cross-service flow with the exact cheat-card commands at `step-17-end`; paste raw curl transcripts; make the data identical in both places. **Scope: M**

### Step 14 — Idempotency keys, pagination, signed webhooks (thinBuild) — money/security
- **WO-14.1 (F1, F3, F4, F11) Build re-enrichment + fix test sequencing.** Missing: every block an excerpt; run-and-see absent for sub-steps 0–2 and 5; sub-steps 3–4 run/commit tests only written in sub-step 6; first win ~4–6h in. At `step-14-end`, transcribe all files (incl. `IdempotencyRecordRepository`, full `WebhookSender`/`WebhookPublisher`, yaml config, all three test classes — moving `WebhookSignerTest` into sub-step 3, `WebhookDeliveryTest` into 4). Re-run per sub-step: Flyway "Successfully applied 2 migrations" lines (first win), compile/test-slice tails, the compose + spring-boot:run + curls for idempotent retry / PageResponse / Deprecation headers. **Scope: L**
- **WO-14.2 (F5) Verification Log §§1–4, 7.** Missing: prose summaries under a "real pasted output" banner. Re-run the suite at `step-14-end` and the clean-room verify; paste surefire per-class lines and clean-room tail. **Scope: M**

### Step 24 — Spring Batch interest accrual + exactly-once capstone (thinBuild) — money
- **WO-24.1 (F1 context, F2) Per-sub-step run-and-see.** Missing: one run command for the whole 18h build (code transcription itself is pure-edit — files exist under `services/demand-account/.../batch/`). At `step-24-end` re-run: any test to capture Flyway `Migrating schema ... to version "4 - batch schema"` lines + the induced `relation "batch_job_instance" does not exist` wrong-output; `./mvnw -pl services/demand-account test -Dtest=InterestAccrualJobTest` (paste read/write/filter/skip counts, plus the FAILED output with skip removed); the capstone test with duplicate-redelivery/dedupe log lines. **Scope: M**

### Step 11 — Concurrency lab (thinBuild) — concurrency
- **WO-11.1 (F1, F2, F9) Complete the code + run evidence.** Missing: no sub-step compiles as shown; third `ConcurrencyToolsTest` test absent so `Tests run: 3` is unreachable; no run-and-see for sub-steps 0–1; no common-wrong-output anywhere. At `step-11-end`, transcribe full pom + five classes + both test files (incl. `awaitQuietly`, `hammer`, `EXPECTED`, printfs). Re-run: `./mvnw -q -pl playground/concurrency-lab validate` (sub-step 0), compile tail (sub-step 1), both test classes (paste `[race]` lines and surefire tails); induce one failure (omit `<module>` line → paste error; run with `UnsafeBalance` → paste assertion failure). **Scope: L**
- **WO-11.2 (F5) Verification Log §4.** Missing: full-repo verify + clean-room asserted with no output. At `step-11-end` run `./mvnw verify` (paste 6-module reactor summary) and the clean-room clone + verify (paste tail). **Scope: M**

### Step 20 — Outbox + Kafka + SSE notifications (thinBuild) — money events
- **WO-20.1 (F1 context, F2, F7) Per-sub-step runs + learner break-it.** Missing: nothing runs until a multi-service manual demo; no break-it exercise. At `step-20-end` (code transcription per F1 is edit-work from the repo): re-run `./mvnw -pl services/demand-account test -Dtest=OutboxWriteTest`, the `OutboxRelayKafkaTest` run, notification tests, and `curl -N http://localhost:8084/api/notifications/stream` (paste one live event frame). Break-it: swap `@TransactionalEventListener(AFTER_COMMIT)` for `@EventListener`, run the rollback test, paste the real failing output, revert (or the `spring-kafka`-without-starter `KafkaTemplate` bean error). Add the real on-topic event JSON for F10. **Scope: L**

### Step 18 — Threat modeling & hardening (thinBuild) — security
- **WO-18.1 (F2, F4) Runnable build + pre-hardening baseline.** Missing: zero run-and-see in the build; first win hours late. On a branch at `step-18-start`: capture the pre-hardening baseline (`curl -i` showing missing headers; the BOLA read of another user's account). At `step-18-end`: `./mvnw -pl services/demand-account -am test -Dtest=SecurityHardeningTest` and `./mvnw -pl services/cif -am test -Dtest=SqlInjectionSafetyTest` (paste tails + the two-beans / Docker-down wrong-outputs); `grep -c "R-001" security/risk-register.md`. **Scope: M**
- **WO-18.2 (F7) Cheat-card `-am` + `-Dtest` command.** Missing: may fail with "No tests were executed!" in upstream modules. Run the exact cheat-card command at `step-18-end`; if it fails, add `-Dsurefire.failIfNoSpecifiedTests=false` or split per-module. **Scope: S**
- **WO-18.3 (F12) Clean-room + smoke evidence.** Run `bash steps/step-18/smoke.sh` and clean-room `make verify`; paste per-check lines + 9-module reactor summary into Verification Log items 4–5. **Scope: S**

---

## Tier 2 — remaining thin builds

### Step 23 — Onboarding orchestration (thinBuild) — worst-scored lesson
- **WO-23.1 (F1, F2) Rebuild as 6–8 full sub-steps with tests.** Missing: three goal-only stubs, zero code; Prove cites `OnboardingOrchestrationTest`/`OnboardingControllerTest`/cif `CustomerControllerTest` never written. At `step-23-end`, transcribe the whole onboarding module + CIF deactivate diff + all three tests (incl. in-process stub servers and `deactivateCalls`). Re-run per sub-step: compile tails, `./mvnw -pl services/onboarding test`, `./mvnw -pl services/cif test -Dtest=CustomerControllerTest` (paste output matching Prove). **Scope: L**
- **WO-23.2 (F7) Play With It live flow.** Missing: no runnable commands; `ACCOUNT_URL` unexplained. Start auth + cif + demand-account + onboarding (exact `./mvnw -pl services/<x> spring-boot:run` per terminal), token curl, onboarding curl (paste real 201 body), stop-demand-account → paste real 502; show where `services.account.url` is overridden. **Scope: M**

### Step 22 — Caching, @Async, ShedLock (thinBuild)
- **WO-22.1 (F1, F5) Full build re-enrichment + per-sub-step wins.** Missing: zero code (12 files); nothing runs until Play With It. At `step-22-end`, transcribe `services/market-info/` (pom with ALL deps, yml, 10 classes, 3 tests). Re-run: `./mvnw -pl services/market-info test -Dtest=MarketCacheTest` etc.; boot vs local Redis + two curls (paste slow-then-fast timings + call-count log line); async test run; two-instance demo excerpt (one node logging "refreshed N rates"). **Scope: L**
- **WO-22.2 (F3) The "Redis read-after-write isn't instant" claim.** Missing: a wrong mechanism taught 4×. Reproduce the original `expected 1 but was 2` flake against real Redis at `step-22-end`; identify the true cause (likely cache stampede — `@Cacheable` without `sync=true`); rewrite the four passages and re-justify/remove the test's `await`. **Scope: M**

### Step 19 — Distributed systems theory lab (thinBuild)
- **WO-19.1 (F1) Inline the lab code + per-sub-step runs.** Missing: zero code for 5 classes + 4 test classes + pom edits. At `step-19-end`, transcribe `playground/distributed-lab`; re-run `./mvnw -pl playground/distributed-lab test -Dtest=<Class>` per sub-step and paste each tail. **Scope: L**
- **WO-19.2 (F4) Verification Log items 3–5.** Missing: "pasted in the commit's verification run" is not pasted. Run `./mvnw verify` at `step-19-end` (10-module reactor summary), `bash steps/step-19/smoke.sh`, and the clean-room clone; paste real tails. **Scope: M**

### Step 15 — API gateway + HTTP interface clients (thinBuild)
- **WO-15.1 (F2, plus F1 context) Run-and-see for sub-steps 0/1/3/4 + wrong-outputs.** Missing: 4 of 6 sub-steps have no command/output; no common-wrong-output anywhere (code transcription per F1 is edit-work from repo files incl. the actuator dep/exposure per F4). At `step-15-end` re-run: `./mvnw -q -pl gateway dependency:resolve`; boot gateway + curl a route; `./mvnw -pl services/demand-account -am compile`; the routing/client tests; induce the deprecated-prefix 404-everything symptom for the wrong-output. **Scope: M**

### Step 26 — Hexagonal restructure (thinBuild)
- **WO-26.1 (F2, F4) Intermediate-state compile proof + baseline win.** Missing: a multi-file package move with no run-and-see at any intermediate state; first run at the very end. On a branch from `step-26-start`: paste the baseline `./mvnw -pl services/notification test` (7 green); replay the move and capture `./mvnw -pl services/notification test-compile` after each sub-step plus one induced `cannot find symbol` / `package ... does not exist` wrong-output; final full test run. **Scope: M**
- **WO-26.2 (F5) Un-doctor the Verification Log.** Missing: `…`-elided, annotated fences under "Real output below". Re-run the module suite at `step-26-end`; paste raw surefire lines; move commentary outside the fence. **Scope: S**

### Step 25 — Refactoring to a thin consumer (thinBuild)
- **WO-25.1 (F1, F3) Build re-enrichment + green-suite discipline.** Missing: zero code in 4 sub-steps; the step's own "refactor under a green suite" rule never exercised. At `step-25-start`: paste the baseline `./mvnw -pl services/notification test`; replay each structural move and re-run the suite per sub-step (paste tails); transcribe the six files + two tests from `step-25-end`. **Scope: M/L**
- **WO-25.2 (F7) Raw surefire lines for the log.** Re-run at `step-25-end`; paste unedited `Tests run: ...` lines; move "(UNCHANGED …)" annotations out of the fence. **Scope: S**

### Step 28 — Testing & quality mastery, custom starter (thinBuild)
- **WO-28.1 (F1, F3) Build re-enrichment + per-sub-step runs.** Missing: zero code blocks across 6 sub-steps; nothing runs during the build. At `step-28-end`, transcribe the tests, `-Pmutation` profile, `libs/common` starter files, `AutoConfiguration.imports`, checkstyle/gates wiring. Re-run per sub-step: `./mvnw -pl services/notification test -Dtest='NotificationServiceTest,NotificationTest'` (first win); the jqwik `tries = 1000` block; PITest score lines (`./mvnw -pl services/notification -Pmutation verify` or equivalent); `-pl libs/common test` + the hello injection test; the slice test; `spotless:apply` + a planted violation failing `checkstyle:check`. Do not reuse Movement-D pastes. **Scope: L**

### Step 27 — ArchUnit + Spring Modulith (thinBuild)
- **WO-27.1 (F2, plus F1 context) Per-sub-step runs.** Missing: first executable command comes after the whole build (code transcription per F1 is edit-work — both test classes + pom diffs exist at `step-27-end`). Re-run: `./mvnw -pl services/notification dependency:tree | grep archunit` (+ Modulith equivalent); `./mvnw -pl services/notification -Dtest=HexagonalArchitectureTest test` (real 4/4 tail + "No tests were executed" and vacuous-pass wrong-outputs); `ModularityTest` run + `ls target/spring-modulith-docs/`. **Scope: M**

### Step 29 — React/TS SPA + gateway CORS (thinBuild) — stack-shift step
- **WO-29.1 (F1 context, F2, F7) Build re-enrichment + per-sub-step runs.** Missing: zero code for ~16 files; no command anywhere between Starting Point and Play With It. At `step-29-end`, transcribe `frontend/**` + gateway diffs; re-run per sub-step: `npm install` + `npm run dev` (paste the Vite ready banner — the first win), `npm run build`, `npx vitest run` (three points), `./mvnw -pl gateway test`; one common-wrong-output each. **Scope: L**
- **WO-29.2 (F8) Verification Log items 7–8.** Run `bash steps/step-29/smoke.sh`, `./mvnw verify` (14-module reactor tail), and clean-room `npm ci && npm run build && npm test`; paste trimmed real output. **Scope: S**

### Step 30 — TanStack Query + RHF/Zod + SSE (thinBuild)
- **WO-30.1 (F1, F2, F4) Build + test authoring re-enrichment.** Missing: goal-only sub-steps; DoD demands 15 tests across 6 files the build never writes; nothing runs until Play With It. At `step-30-end`, transcribe `queries.ts`, `client.ts` diff, `AccountPanel.tsx`, `TransferForm.tsx`, `useNotificationStream.ts`, `LiveNotifications.tsx`, gateway yml diff, `renderWithProviders.tsx`, `setup.ts` EventSource stub, and all 6 test files into the sub-steps. Re-run: `npm test` baseline after `QueryClientProvider` (first win), targeted `npx vitest run <file>` per sub-step showing the growing test count, `npm run build`. **Scope: L**

---

## Tier 3 — non-thin lessons with needsRun findings (evidence & sequencing fixes)

### Step 13 — Errors/ProblemDetail/OpenAPI/filters (money-path API)
- **WO-13.1 (F2) Sub-step 2's untrue run.** The promised green `TransferControllerTest` run fails until sub-step 6. On a replay branch from `step-13-start`: either move the assertion updates forward (re-run to capture green) or capture the real RED failing output and reframe honestly. **Scope: M**
- **WO-13.2 (F10) Missing run-and-see for sub-steps 0/1/4.** Capture: `dependency:resolve` tail; `./mvnw -q -pl services/demand-account -am compile`; restart + curl any endpoint and grep `X-Request-Id`. **Scope: S**
- **WO-13.3 (F11) Balance-leaking error detail.** Change the 422 detail to omit the balance; re-run the overdraw flow + slice test at the edited state; paste new output into Prove §3. **Scope: S**

### Step 08 — JPA + Flyway + Testcontainers
- **WO-08.1 (F3) Sub-step 1's asserted-but-unverified `verify` failure.** Replay to end of sub-step 1 (pom + `CifApplication` only); run `./mvnw -pl services/cif -am verify`; paste real result and rewrite the predict answer. **Scope: S**
- **WO-08.2 (F4) Run-and-see for sub-steps 2–6, 8.** Capture `./mvnw -pl services/cif -DskipTests compile` per state + a `ls src/main/resources/db/migration` for sub-step 3; one induced wrong-output each. **Scope: M**
- **WO-08.3 (F6) The impossible "silently swaps in H2" pitfall.** Delete `@AutoConfigureTestDatabase(replace = NONE)` at `step-08-end`; run `-Dtest=CustomerRepositoryTest`; rewrite pitfall + troubleshooting to observed behavior. **Scope: S**
- **WO-08.4 (F5) Early boot option.** Move compose.yaml earlier; run `docker compose -f services/cif/compose.yaml up -d` + `spring-boot:run` at post-sub-step-8 state; paste Flyway + Tomcat lines. **Scope: S**
- **WO-08.5 (F12) Raw curl transcripts.** Re-run the four curls live at `step-08-end`; replace stylized `->` lines in sub-step 12 and Prove §2 with real `curl -i` heads. **Scope: S**

### Step 01 — Setup & first Spring Boot service (entry lesson)
- **WO-01.1 (F4) Sub-step 0: scaffold + git init from an empty folder.** Author and actually execute the bootstrap sequence (copy step-01-start scaffold, `git init`, `.gitignore`/`.env.example`/`.env`, `git status`, `git check-ignore -v .env`, initial commit); paste real output. **Scope: M**
- **WO-01.2 (F1) Title-scope decision.** If option (a) — teach terminal/Linux/Git — every new run-and-see needs real captured output (branch/merge-conflict/rebase walkthrough). If option (b) rescope, this becomes pure edit. **Scope: L (option a) / S (option b)**
- **WO-01.3 (F5) `/actuator/health` groups mismatch.** Run `curl -s localhost:8080/actuator/health` against the exact sub-step 7 config at `step-01-end`; paste the true body in all three locations (or add the probes key, re-run, paste). **Scope: S**
- **WO-01.4 (F6) Reordered Maven tail.** Re-run `./mvnw -B verify`; paste the genuine tail (reactor summary incl. parent, then BUILD SUCCESS) in sub-step 8 + Verification Log. **Scope: S**
- **WO-01.5 (F7, F8) Sub-step 2 and 5 gaps.** Run `./mvnw -B validate` (or `help:effective-pom | head`) for sub-step 2; capture the real no-web-starter run log for sub-step 5. **Scope: S**

### Step 10 — Postgres internals lab
- **WO-10.1 (F5) Author the missing Play-With-It.** Start the throwaway container; run 2–3 experiments from `steps/step-10/queries.sql` in two psql sessions (xmin/xmax across UPDATE; hand-made non-repeatable read); paste real psql output. **Scope: M**
- **WO-10.2 (F6) Sub-step 0 run-and-see.** `./mvnw -pl services/cif test-compile`; paste tail + one induced wrong-package error. **Scope: S**
- **WO-10.3 (F7) Verification Log §9–10.** Run `bash steps/step-10/smoke.sh` + clean-room fresh clone at `step-10-end` (`make doctor`, `./mvnw -pl services/cif -am verify`, `git rev-parse step-10-end step-11-start`); paste tails. **Scope: M**

### Step 02 — Java fundamentals
- **WO-02.1 (F2) Verification Log format.** Re-run `./mvnw -B verify` at `step-02-end`; replace the invented `(DisplayName: ...)` lines with the genuine tail in both locations; fix the two prose claims. **Scope: S**
- **WO-02.2 (F4) EnumMap determinism fix.** Change `countByType` to an `EnumMap` collector; re-run the demo + `verify`; refresh pasted outputs (or soften wording — then pure edit). **Scope: S**
- **WO-02.3 (F6) jshell interludes.** Run the three jshell snippets against compiled classes; paste real `$1 ==> ...` output after sub-steps 2/5/9. **Scope: S**
- **WO-02.4 (F11) Common-wrong-outputs for six sub-steps.** Reproduce each induced compile error once; paste genuine `[ERROR]` lines. **Scope: S**

### Step 09 — JPA relationships & optimistic locking
- **WO-09.1 (F2) Verify the lock path actually taken.** Run `OptimisticLockingTest` with SQL logging at `step-09-end`; confirm merge-time vs flush-time path; correct prose + sequence diagram (or restructure the test) and paste observed SQL. **Scope: M**
- **WO-09.2 (F3) Verification Log §3/§5.** Run `-Dtest=OptimisticLockingTest` and `bash steps/step-09/smoke.sh`; paste real surefire block + smoke output (optionally real `Hibernate:` SQL for §2). **Scope: S**
- **WO-09.3 (F4) Run-and-see for sub-steps 1/2/4.** `./mvnw -pl services/cif -am test-compile` at each replayed state; paste tails + one wrong-output. **Scope: S**

### Step 06 — Auto-configuration
- **WO-06.1 (F4) The break-it that probably goes RED, not green.** Run the rename-`.imports` experiment at `step-06-end` (`verify` + targeted `-Dtest=GreetingAutoConfigurationTest`); paste the real `UnsatisfiedDependencyException` outcome and rewrite the break-it honestly. **Scope: S**
- **WO-06.2 (F5) Sub-step 6 actuator outputs.** Re-run the two `jq` commands + four follow-up curls against a live hello-service; paste literal outputs in sub-step 6 and Verification Log §3. **Scope: S**

### Step 03 — HTTP from first principles
- **WO-03.1 (F2) `-q` vs `[INFO]` mismatch.** Drop `-q` from sub-steps 4–5, re-run each exact command, paste the true tails. **Scope: S**
- **WO-03.2 (F6) jshell first win.** Run `UrlAnatomy.of(...)` in jshell; paste the record toString. **Scope: S**
- **WO-03.3 (F7) Host header port fix.** Change RawHttpDemo to `Host: host:port`; re-run demo + loopback test; sync `step-03-end` reference code. **Scope: S**

### Step 05 — Spring IoC/DI lab
- **WO-05.1 (F5) Run-and-see for 7 of 11 sub-steps.** Capture `./mvnw -q -pl playground/spring-lab -am compile`/`validate` at each replayed state + one wrong-output each. **Scope: M**
- **WO-05.2 (F6) First-win run of the empty context.** Run `spring-boot:run` at the sub-step-2 state; paste the real boot-and-exit log. **Scope: S**

### Step 07 — AOP & proxies
- **WO-07.1 (F5) Real `curl -i` output.** Start the app at `step-07-end`; run both curls; paste genuine status line + key headers + body into sub-step 5 and Prove §3. **Scope: S**
- **WO-07.2 (F6) Make the headline experiment runnable.** Add the `/{id}/summary` handler (temp or permanent); curl it; paste the app-log excerpt showing one AUDIT pair and no inner `findById`. **Scope: S**

### Step 04 — JVM internals
- **WO-04.1 (F7) Sub-step 2 placeholder output.** Run `java -cp ... AllocationDemo 5000000`; paste the real line (checksum 637493856) + the missing wrong-output note. **Scope: S**
- **WO-04.2 (F2) Create `solutions/step-04/`** (BoxingBytecode, PromotionDemo, EscapeBenchmark skeleton), verify each runs and shows the claimed behavior — or de-reference (pure edit). **Scope: M (create) / S (de-reference)**

---

## Coverage checklist

| Step | needsRun findings covered | thinBuild re-enrichment |
|---|---|---|
| 01 | F1, F4, F5, F6, F7, F8 | — |
| 02 | F2, F4, F6, F11 | — |
| 03 | F2, F6, F7 | — |
| 04 | F2, F7 | — |
| 05 | F5, F6 | — |
| 06 | F4, F5 | — |
| 07 | F5, F6 | — |
| 08 | F3, F4, F5, F6, F12 | — |
| 09 | F2, F3, F4 | — |
| 10 | F5, F6, F7 | — |
| 11 | F5, F9 | ✔ WO-11.1 |
| 12 | F2, F5, F9 | ✔ WO-12.1 |
| 13 | F2, F10, F11 | — |
| 14 | F3, F5, F11 | ✔ WO-14.1 |
| 15 | F2 | ✔ WO-15.1 |
| 16 | F4, F10 | ✔ WO-16.1 |
| 17 | F6, F10 | ✔ WO-17.1 |
| 18 | F2, F4, F7, F12 | ✔ WO-18.1 |
| 19 | F1, F4 | ✔ WO-19.1 |
| 20 | F2, F7 | ✔ WO-20.1 |
| 21 | F1, F9 | ✔ WO-21.1 |
| 22 | F1, F3, F5 | ✔ WO-22.1 |
| 23 | F1, F2, F7 | ✔ WO-23.1 |
| 24 | F2 | ✔ WO-24.1 |
| 25 | F1, F3, F7 | ✔ WO-25.1 |
| 26 | F2, F4, F5 | ✔ WO-26.1 |
| 27 | F2 | ✔ WO-27.1 |
| 28 | F1, F3 | ✔ WO-28.1 |
| 29 | F2, F7, F8 | ✔ WO-29.1 |
| 30 | F1, F2, F4 | ✔ WO-30.1 |
