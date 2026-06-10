# ADR-0015: Spring Batch EOD interest accrual (Flyway-owned JobRepository) + the Phase-D capstone

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 24 — Spring Batch & batch processing; **End of Phase D**

## Context
A bank runs **end-of-day** batch work — interest accrual, statements, reconciliation — over large datasets,
where one bad record must not abort the night and a crashed run must be recoverable. Step 24 adds Spring Batch
to demand-account (where the accounts/ledger + Postgres already live) with a chunk-oriented **interest-accrual**
job, and closes Phase D with a capstone that ties the phase together.

## Decision

### 1. Spring Batch in demand-account; Flyway owns the JobRepository schema
`spring-boot-starter-batch` (Spring Batch **6.0.3**, Boot-managed). Spring Boot auto-configures the
`JobRepository`, `JobLauncher`, and transaction manager — no `@EnableBatchProcessing` (adding it would turn
Boot's autoconfig off). The Batch metadata tables (`BATCH_*`) are created by a **Flyway migration (V4)** copied
verbatim from `spring-batch-core/schema-postgresql.sql`, and Batch's own initializer is disabled
(`spring.batch.jdbc.initialize-schema=never`) — because Flyway owns the schema (Hibernate `ddl-auto=validate`),
and Batch's `initialize-schema=always` would re-run plain `CREATE TABLE`s every startup. Jobs do **not** run on
startup (`spring.batch.job.enabled=false`) — they're launched explicitly (a test, an endpoint, or an EOD
`@Scheduled`+ShedLock trigger reusing Step 22).

### 2. A chunk-oriented, fault-tolerant interest-accrual job
`interestAccrualJob` / `interestAccrualStep`: a `RepositoryItemReader` pages over accounts (deterministic
`id ASC` order — restartable), an `InterestProcessor` computes the interest or **filters** the account
(returns `null` for zero/negative balances), and an `InterestWriter` credits the account + appends a ledger
entry per chunk (re-reading with the Step-12 pessimistic lock so a concurrent transfer can't lose the update).
Fault tolerance: `skip(InterestSkipException)` so one bad record doesn't fail the run, and
`retry(OptimisticLockingFailureException)` to ride a transient lock conflict against live traffic. (We kept the
`chunk(size, txManager)` builder — deprecated-for-removal in Batch 6.0 in favour of the new
`ChunkOrientedStepBuilder`, whose fault-tolerance API is still settling — and noted the migration; it compiles
and runs on the pinned version.)

> Simplification: interest is posted as income to the customer only; the bank-side contra GL entry is out of
> scope here (double-entry was Step 12). This step is about chunking/skip/retry, not bookkeeping completeness.

### 3. The Phase-D capstone — exactly-once *effect* under a forced retry
A single test traces a payment end-to-end on real Postgres + Redpanda, consolidating the phase:
**Idempotency-Key** (a replayed transfer moves money once) → **Outbox** (the transfer atomically wrote the
event; the relay publishes it) → **at-least-once + idempotent consumer** (we force a duplicate redelivery and
dedupe by eventId → applied exactly once). The assertions are scoped to *this* payment's `txId` so they're
robust to the shared topic carrying other tests' events.

## Consequences
- ✅ A real EOD job with chunking + skip + retry, proven on Postgres (interest credited, zero-balance filtered,
  sentinel skipped, step counts asserted).
- ✅ The JobRepository schema is versioned and reproducible (Flyway), not auto-initialized — production-correct.
- ✅ The Phase-D capstone demonstrates exactly-once *effect* under a forced duplicate, end-to-end.
- ✅ **End of Phase D** 🎖️ — a secured, event-driven + batch microservices backend (transactions, caching,
  Saga money movement, real-time push, orchestration, batch) with the distributed-systems theory beneath it.
- ⚠️ The interest job's bank-side double-entry is simplified; full GL/event-sourced ledger is Phase J (Step 52).
- ⚠️ EOD scheduling (an `@Scheduled`+ShedLock trigger) is described but the job is launched explicitly in tests;
  wiring the timer is a small follow-up (reuse Step 22).
- 🔁 Partitioned/parallel batch + an OLAP/warehouse read path are stretch goals; statements/reconciliation jobs
  follow the same chunk template. Phase E (Step 25+) begins next.
