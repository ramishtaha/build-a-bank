# 🧳 Capsule — Step 2

**Exists now:** parent POM (Java 25 / Spring Boot 4.0.6, pinned) · `services/hello` (Step 1, 2 tests) · `playground/java-basics` (new: 10 main classes, 4 test classes, 16 tests). No HTTP endpoints, no DB, no Docker yet (first endpoint Step 8/13, first DB Step 8). `./mvnw -B verify` = BUILD SUCCESS, 18 tests total (java-basics 16 + hello 2).

**This step added:**
- `playground/java-basics` module (plain Java, no Spring; JUnit 5 + AssertJ via parent BOM) — per ADR-0003
- `Money` record (BigDecimal, HALF_EVEN banker's rounding, String-constructed, cross-currency arithmetic throws)
- `Customer` record (LocalDate dateOfBirth, `ageOn`) · sealed `Account` + Checking/Savings records · `AccountInfo` exhaustive pattern switch (no `default`)
- `Transaction` record + `TransactionType` enum (UTC `Instant` timestamps) · `InsufficientFundsException` (unchecked, not thrown until Step 12)
- generic `Repository<T,ID>` + `InMemoryCustomerRepository` (ConcurrentHashMap; `findById` → Optional)
- `TransactionAnalytics` (streams: reduce / groupingBy+counting / max→Optional) · `TimeExamples` (Instant/ZonedDateTime/Duration) · `Step2Demo` (prints net 1124.50 USD)

**Gotchas:**
- `-q` Maven runs print nothing on success — silence IS success; `[ERROR]` lines = failure
- Construct BigDecimal from String, never double; assert money with `isEqualByComparingTo` (`.isEqualTo` fails on scale: 2000.0 vs 2000.00)
- `countByType` returns a HashMap — enum iteration order not guaranteed; tests assert entries, not order
- Windows: `.\mvnw.cmd`; `-pl` paths use forward slashes on every OS
- Repository operations are atomic but compound read-modify-write is NOT safe (deep dive Step 11/12)

**Callback hooks:**
- `Repository<T,ID>` (`findById`→Optional, `save`) is the exact shape Spring Data JPA generates in Step 8
- `Money`/`Transaction` seed the real ledger (Step 12); `InsufficientFundsException` → HTTP 4xx ProblemDetail via `@ControllerAdvice` in Step 13
- Ground rules cited by every later step: money = BigDecimal + HALF_EVEN; time = UTC `Instant`, zone only at the display edge

**Next step starts:** `step-02-end` == `step-03-start` (tag). Green: `./mvnw -B verify` (18 tests: java-basics 16 + hello 2), `Step2Demo` report (net 1124.50 USD, 3.25% APR), `steps/step-02/smoke.sh` PASSED.
