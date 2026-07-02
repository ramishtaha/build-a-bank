# 🧳 Capsule — Step 24

**Exists now:** 13 modules, `./mvnw verify` BUILD SUCCESS; demand-account (:8082) has **44** tests (42 prior + batch + capstone), Postgres schema at Flyway **V4**, Kafka topic `transfers.completed`. End of Phase D.

**This step added:**
- `spring-boot-starter-batch` in demand-account; `V4__batch_schema.sql` (BATCH_* JobRepository tables, verbatim from `schema-postgresql.sql`); yml: `spring.batch.job.enabled=false`, `spring.batch.jdbc.initialize-schema=never`.
- `interestAccrualJob`: `RepositoryItemReader` (id ASC, page 10) → `InterestProcessor` (null=filter, "SKIP"→`InterestSkipException`) → `InterestWriter` (pessimistic re-read, credit, ledger); chunk 10, `.faultTolerant().skip(InterestSkipException).skipLimit(100).retry(OptimisticLockingFailureException).retryLimit(3)`.
- `InterestAccrualJobTest` (real Postgres): COMPLETED; read 4 / write 2 / filter 1 / processSkip 1; ACC-1 1000→1000.10, ACC-2 500→500.05.
- 🎓 `PaymentExactlyOnceCapstoneTest` (real Postgres+Redpanda): Idempotency-Key transfer (1 movement) → Outbox→Kafka → forced duplicate → delivered ≥2×, **applied exactly once**.
- ADR-0015; `steps/step-24/smoke.sh` (runs both tests). No `requests.http` (no HTTP surface).

**Gotchas:**
- Batch 6 packages moved: item layer `org.springframework.batch.infrastructure.item.*`; `Job`/`Step` under `…core.job`/`…core.step`. `chunk(size, txManager)` is deprecated-for-removal in 6.0.
- No `@EnableBatchProcessing` — it turns Boot's Batch autoconfig OFF. `initialize-schema=never` requires V4, else `relation "batch_job_instance" does not exist`.
- Jobs launch explicitly with a unique `runId` JobParameter — a COMPLETED JobInstance won't re-run; `job.enabled=false` keeps jobs from running at every context start.
- Retry covers `OptimisticLockingFailureException` only; a pessimistic lock timeout (`PessimisticLockingFailureException`) is a sibling, not covered (common parent: `ConcurrencyFailureException`).
- Cached test contexts share a Redpanda container/topic — scope Kafka assertions to the payment's `txId`.

**Callback hooks:**
- Exactly-once **effect** = at-least-once + Outbox + idempotent consumer (dedupe by eventId) — revisited at Step 52 (event sourcing) and Step 58 (cloud-native capstone).
- EOD trigger pattern: `@Scheduled` + ShedLock (Step 22) launching the job with the run date as JobParameter (challenge; not wired).
- JobRepository (Flyway V4) makes runs restartable — the reader's `id ASC` sort is what keeps paging deterministic on restart.

**Next step starts:** `step-24-end == step-25-start` (Phase E begins). Green: `./mvnw verify` (13 modules), demand-account 44 tests, §12.3 mutation reverted, clean-room build, `smoke.sh` PASSED.
