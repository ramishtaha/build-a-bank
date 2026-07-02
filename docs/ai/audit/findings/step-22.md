# Step 22 audit - swe:5 pedagogy:4 adhd:3 structure:4 - thinBuild:true

## Strengths

- Honest, specific Verification Log: real pasted test output, a §12.3 mutation check with the actual failure output and an explicit revert, and a §12.8 honesty note that precisely scopes what the ShedLock test proves (lock-store level) vs. what it does not (N real processes).
- Strong misconception coverage in prose: self-invocation/proxy rule (tied back to Step 7), the "@Scheduled runs on every pod" gotcha, lockAtMostFor vs lockAtLeastFor, cache-as-CQRS framing — with prior-step callbacks (7, 11, 20, 21) and explicit interview framing throughout.
- Tight, well-organized Orient/Understand: complete 30-second table, skip-test, cheat card with headline diagram, three focused under-the-hood sections; low wall-of-text risk in the sections that exist.

## Missing spine

- Build sub-step micro-anatomy for ALL 3 sub-steps: complete code (zero code blocks in the entire build), exact file locations, line-by-line explanations, per-sub-step under-the-hood, run-and-see (command + expected output + common-wrong-output), checkpoints, per-sub-step commits (only one commit, at the end of sub-step 3)
- Predict-then-run for sub-steps 2 and 3 (only sub-step 1 has a predict)
- Interactivity toolkit: type-it-yourself, break-it-on-purpose, knowledge-checks, you-are-here markers — none present in the build
- Analogy in the Big Idea (diagram present, analogy absent)
- What-we-will-build Mermaid diagram at the build's open (only the files tree appears, in a "B→C bridge" before the build header; the flow diagram lives in Understand)
- Build-closing sequence diagram of the flow built
- Inline flashcards (3-5) — recap item (g) only points at `docs/flashcards.md`
- Session plan / per-movement and per-sub-step time-boxes / re-entry support (14-hour step, no sittings)

## Findings

### F1: Build contains zero code — the lesson cannot be completed from the lesson

**Severity:** high
**Lens:** swe
**Location:** C · Build, Sub-steps 1–3 (lines 202–218)
**needsRun:** true
**Issue:** The build lists 12 new files in the files tree but shows none of them. Each sub-step is a 2–4 line stub (Goal sentence only). A beginner cannot write `pom.xml` (deps described only as "cache + data-redis + shedlock(-redis) pinned 6.10.0" — omitting web, actuator, testcontainers, awaitility), `AsyncConfig`, `RedisLockConfig`, the SpEL cache keys, or the `market.scheduling.enabled` gate from what is on the page. The real code exists in `C:/Users/ramishtaha/Desktop/Claude/build-a-bank/services/market-info/` (e.g. `MarketRateService` uses `key = "#base + '/' + #quote"`, `application.yml` sets `time-to-live: 60s`) but the lesson never shows any of it.
**Fix:** Expand each sub-step to the full micro-anatomy using the actual repo files: complete listings with file-path header comments and all imports (root `pom.xml` diff registering the module; `services/market-info/pom.xml` with ALL dependencies; `application.yml`; all 10 classes; the three test classes), each followed by line-by-line explanation, under-the-hood, and a real run-and-see with exact command and pasted output (e.g. `./mvnw -pl services/market-info test -Dtest=MarketCacheTest`). Run each command and paste the true output — do not invent it.

### F2: Micro-anatomy scaffolding absent for every sub-step

**Severity:** high
**Lens:** structure
**Location:** C · Build, Sub-steps 1–3 (lines 202–218)
**needsRun:** false
**Issue:** No sub-step has the contract's ordered anatomy: no exact-file-location lines, no checkpoints, no run-and-see slots, no common-wrong-output, predict only in sub-step 1, pitfall only in sub-step 3, and a single commit covering all three sub-steps.
**Fix:** Restructure each sub-step under the fixed template Goal → file location → code → line-by-line → under-the-hood → predict-then-run → run-and-see → checkpoint → commit → pitfall. Add predicts to sub-steps 2 ("will `Thread.currentThread().isVirtual()` be true inside warm()? why?") and 3 ("with scheduling gated off in tests, what makes the lock test meaningful?"); add a checkpoint question to each sub-step; split the single commit into one per sub-step (e.g. `feat(market-info): cached FX read model`, `feat(market-info): async cache warming on virtual threads`, `feat(market-info): ShedLock-guarded scheduled refresh`). (Real command output belongs to F1's run.)

### F3: "Redis read-after-write isn't instant" claim is technically wrong and taught four times

**Severity:** high
**Lens:** swe
**Location:** "Under the Hood: Spring Cache on Redis" (lines 136–139); Sub-step 1 predict answer (line 206); Verification bullet 1 (line 255); Troubleshooting bullet 1 (line 314)
**needsRun:** true
**Issue:** The lesson teaches that "a just-written entry becomes readable after a small round-trip — read-after-write of a freshly-populated entry isn't instant" as "a genuine eventually-consistent property." Single-node Redis is read-after-write consistent: `RedisCacheWriter.put` is a synchronous SET whose round-trip completes inside the first `getRate` call, before the aspect returns to the caller — a strictly-subsequent read cannot miss. The `expected 1 but was 2` flake the lesson reports is far more plausibly a concurrent double-miss (cache stampede — `@Cacheable` without `sync=true` runs the method in both concurrent callers) or test-side concurrency with the async warmer.
**Fix:** Reproduce the original flake against real Redis, identify the actual cause, and rewrite all four passages to teach the true mechanism — most likely replacing the "network visibility" story with the cache-stampede/`@Cacheable(sync=true)` lesson, and re-justifying (or removing) the `await` in `MarketCacheTest` accordingly.

### F4: 14-hour step with no session plan, time-boxes, or re-entry support

**Severity:** high
**Lens:** adhd
**Location:** A · Orient effort row (line 36) and throughout C · Build
**needsRun:** false
**Issue:** The only time information is "≈ 14 hours focused." There are no planned sittings, no per-movement or per-sub-step time-boxes, no named save points, and no re-entry lines — an ADHD learner has no way to plan or resume this step.
**Fix:** Add a "Session plan" box to Orient: 5–7 sittings of ~2–3h each with named save points mapped to commits (e.g. S1: Orient+Understand; S2: sub-step 1 through its commit; S3: sub-step 2; S4: sub-step 3 + Play With It; S5: Prove; S6: Apply+Review). Add a time estimate to each movement header and each sub-step, and end every sub-step with a re-entry line: "Stopping here? You have <X> working and committed; next session starts at Sub-step <N>, first action: <open file / run command>."

### F5: No first win and near-zero reward loop in the build

**Severity:** medium
**Lens:** adhd
**Location:** C · Build — nothing runs until "🎮 Play With It" (line 221) after all three sub-steps
**needsRun:** true
**Issue:** The learner writes an entire service (12 files) before anything visibly runs. The first runnable moment is Play With It, hours in; each sub-step ends with no observable result.
**Fix:** End sub-step 1 by booting the service against local Redis and running two curls, pasting the real slow-then-fast timings (first ~800ms, second ~5ms) plus the `RateProvider` call-count log line; end sub-step 2 with the async test run showing the virtual-thread assertion pass; end sub-step 3 with the two-instances demo excerpt (only one node logging "refreshed N rates"). Run these for real and paste actual output.

### F6: Interactivity toolkit missing from the build

**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, Sub-steps 1–3 (lines 202–218)
**needsRun:** false
**Issue:** One predict-then-run is the only interactive element. No type-it-yourself, no break-it-on-purpose, no knowledge-checks, no you-are-here markers — so no scaffold fading is possible and there is no formative check between the Goal stubs and the Verification Log.
**Fix:** Add "🧭 You are here: Sub-step X of 3" lines; add a break-it-on-purpose to sub-step 1 (comment out `@EnableCaching` — or call `getRate` via `this.` from another method — rerun the curl loop and watch the upstream call count climb; mirrors the §12.3 mutation); mark sub-step 2's short `AsyncConfig` as type-it-yourself (fading from the fully-worked sub-step 1); add a knowledge-check after sub-step 3 ("crash the lock holder mid-tick — what happens at the next tick, and which setting bounds the wait?").

### F7: Missing build-opening diagram and build-closing sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** C · Build open (line 196) and close (lines 233–235)
**needsRun:** false
**Issue:** The contract requires the build to open with a what-we-will-build Mermaid diagram + files tree and close with a sequence diagram of the flow built. The tree sits in a "B→C bridge" before the build header, the only flow diagram is in Understand, and there is no closing sequence diagram.
**Fix:** At the top of C, add a small what-we-will-build Mermaid (controller → @Cacheable service → RateProvider; RateRefreshJob → ShedLock → @CachePut; CacheWarmer → @Async) and move the files tree under it. Before "The Finished Result," add a `sequenceDiagram`: Client→MarketController→MarketRateService (cache hit returns early / miss calls RateProvider then stores), plus a scheduled-tick lane: RateRefreshJob→Redis (acquire lock)→refreshRate (@CachePut).

### F8: Flashcards not in the lesson

**Severity:** medium
**Lens:** structure
**Location:** F · Recap item (g) (line 339)
**needsRun:** false
**Issue:** The recap says flashcards are "appended to `docs/flashcards.md`" but the lesson itself contains none; the contract requires 3–5 inline.
**Fix:** Inline 3–5 Q/A flashcards under item (g), keeping the pointer: (1) cache-aside on hit vs miss; (2) why a cache is an eventually-consistent CQRS read model; (3) @Scheduled on 3 pods — what happens and the fix; (4) lockAtMostFor vs lockAtLeastFor; (5) when virtual threads help (and when they don't).

### F9: Stretch exercise promises a reference solution that does not exist

**Severity:** medium
**Lens:** swe
**Location:** E · Your Turn, stretch bullet (line 304)
**needsRun:** false
**Issue:** "(reference solution in `solutions/step-22/`)" — the repo's `solutions/` directory contains only `step-01`. A learner who attempts the stretch and goes looking for the solution hits a dead end.
**Fix:** Remove the parenthetical (or replace with "no reference solution — model it on the Step 20 consumer wiring: consume `rates.updated`, call `marketRateService.refreshRate(base, quote)`"), or actually build and commit `solutions/step-22/` and keep the claim.

### F10: No analogy in the Big Idea

**Severity:** medium
**Lens:** pedagogy
**Location:** B · "The Big Idea" (lines 105–120)
**needsRun:** false
**Issue:** The contract's Understand movement requires big idea + diagram + analogy; the diagram is present but no analogy anchors the three levers for a beginner.
**Fix:** Add one short analogy paragraph after the numbered list, e.g.: a hotel front desk keeps a printed FX-rate sheet at the counter (cache) instead of phoning the bank for every guest; a porter restocks brochures in the background while guests are being served (@Async); and each hour exactly one designated clerk — whoever grabs the "rate-sheet duty" token first — reprints the sheet, so three clerks don't all phone the bank (ShedLock).

### F11: Play With It commands fail on Windows (the course's own stated environment)

**Severity:** low
**Lens:** swe
**Location:** C · "Play With It" (lines 222–226)
**needsRun:** false
**Issue:** `REDIS_HOST=localhost ./mvnw -pl services/market-info spring-boot:run` is bash-only inline-env syntax; the cheat card notes `.\mvnw.cmd` for Windows but never the env-var difference, and this is a Windows repo.
**Fix:** Add a PowerShell variant line under the bash block: `$env:REDIS_HOST='localhost'; .\mvnw.cmd -pl services/market-info spring-boot:run` (and note `Remove-Item Env:REDIS_HOST` to unset). Note that `REDIS_HOST` defaults to `localhost` in `application.yml`, so the variable is optional when Redis is local.

### F12: Load-bearing config/tokens referenced but never taught

**Severity:** low
**Lens:** pedagogy
**Location:** "Play With It" bullet 3 ("the 60s TTL", line 231); bullet 1 (`GET /actuator/caches`, line 229); Sub-steps 1 and 3 (cache keys never mentioned)
**needsRun:** false
**Issue:** The 60s TTL appears for the first time as something to "wait past" without ever being configured on-page (`spring.cache.redis.time-to-live: 60s`); `/actuator/caches` is used without noting the required `management.endpoints.web.exposure.include: caches`; and SpEL cache keys are never discussed even though the real code depends on `@Cacheable` and `@CachePut` sharing the identical `key = "#base + '/' + #quote"` — a classic silent-bug pitfall.
**Fix:** When adding the code listings (F1), add explicit line-by-line callouts for the TTL line, the actuator exposure line, and a dedicated explanation + pitfall box on default key generation (SimpleKey for multi-arg methods) and why `@CachePut`'s key must exactly match `@Cacheable`'s or the refresh writes a different entry that reads never hit.
