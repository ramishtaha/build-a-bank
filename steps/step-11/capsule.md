# 🧳 Capsule - Step 11

**Exists now:**
- 6 Maven modules green (`./mvnw verify` BUILD SUCCESS), incl. services/cif (Testcontainers) + new `playground/concurrency-lab` (pure JVM — no HTTP surface, no ports)
- concurrency-lab: **8** tests — `LostUpdateRaceTest` (5) + `ConcurrencyToolsTest` (3)

**This step added:**
- `playground/concurrency-lab` module (pure JUnit 5 + AssertJ, no Spring) + root-pom `<module>` entry (ADR-0003 playground convention)
- `Balance` interface + `UnsafeBalance` / `SynchronizedBalance` / `AtomicBalance` / `LongAdderBalance`
- `LostUpdateRaceTest`: deterministic lost update (CyclicBarrier → balance=1 not 2); UnsafeBalance 8×100k lost ~609k of 800k; three fixes exact at 800k
- `ConcurrencyToolsTest`: virtual threads 10k/10k · CompletableFuture `thenCombine` · `Semaphore(3)` capped at 3
- `steps/step-11/smoke.sh`

**Gotchas:**
- Raw `&` in a pom `<description>` → `Non-parseable POM` (write "and" or `&amp;`)
- Unsafe-counter assertion is `<= EXPECTED` on purpose — `<` would be flaky (§12.5); the barrier test is the deterministic proof
- Virtual threads do NOT make racy code safe; carrier pinning inside `synchronized` varies by JDK (we pin 25 — verify yours)
- JCStress is verify-adjacent (§12.8), NOT wired into `./mvnw verify`; full-repo verify needs Docker (cif Testcontainers) but the lab itself doesn't

**Callback hooks:**
- Step 9 `@Version` / Step 10 MVCC+`FOR UPDATE` / Step 11 `synchronized`+CAS = the same lost-update problem at three layers
- Course-wide 🧵 thread-safety notes point back here: confinement / immutability / synchronization
- §12.3 mutation check: remove `synchronized` → `expected 800000 but was 145225` → revert; Step 12 reuses these tools + lock ordering on the money ledger

**Next step starts:** `step-11-end` == `step-12-start`; green: full-repo `./mvnw verify` (6 modules), 8 lab tests, `smoke.sh` PASSED.
