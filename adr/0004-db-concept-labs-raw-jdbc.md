# ADR-0004: Database concept labs use raw JDBC on a shared Testcontainers Postgres

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 10 — Relational Databases Up Close

## Context
Step 10 teaches the relational engine *up close*: reading `EXPLAIN`/query plans (seq vs index vs index-only),
indexing & covering indexes, isolation anomalies (dirty/non-repeatable/phantom) **and write skew**, MVCC snapshots,
declarative partitioning + pruning, online (zero-downtime) schema change, and HikariCP pool internals. Per §12 every
claim must be proven by real, pasted output, and per the Domain-5 depth contract these must run on **real Postgres**.

The question was *how* to make these proofs executable and deterministic without polluting the CIF service:

1. **Where to run them.** Reuse the existing CIF `@ServiceConnection` Spring context, or stand up a dedicated Postgres.
2. **Through what API.** Spring Data/Hibernate, or raw JDBC.
3. **What schema.** Extend CIF's Flyway-owned `customer` schema, or use throwaway lab tables.

Key tensions: (a) Hibernate/Spring *hide* the exact mechanics this step exists to reveal (it auto-manages
transactions, isolation, flushing, connection borrowing); (b) demonstrating isolation anomalies and pool exhaustion
needs **multiple concurrent connections at chosen isolation levels** and a **tiny, controllable pool**, which fights
Spring's single-managed-transaction-per-test model and context-cache reuse; (c) shipping lab tables (`txn`,
`linked_account`, monthly partitions) into CIF's production schema via Flyway would be misleading — they are teaching
fixtures, not customer-master data.

## Decision
Implement the Step-10 labs as **plain JUnit tests using raw JDBC** (`java.sql.Connection`, `DriverManager`) against a
**single shared singleton Testcontainers Postgres** (`postgres:17-alpine`, pinned), in `services/cif/src/test/.../dblab/`.

- A `DbLab` base class starts **one** container for the JVM (the Testcontainers singleton-container pattern) and
  exposes helpers: `openTx(isolation)` (autocommit off — we drive transactions by hand), `openAuto()`, `explain(...)`,
  `exec(...)`, `scalar(...)`.
- Each lab creates and drops **its own** lab tables; the Flyway-owned `customer` schema is never touched.
- HikariCP is exercised directly (a `HikariDataSource` sized `maximumPoolSize=2`, `connectionTimeout=500ms`) so pool
  saturation is observable, not abstracted away.

## Consequences
- ✅ The mechanics are **visible**: we set the isolation level explicitly, interleave two live transactions, and read
  raw plan text — exactly what an engineer does in `psql`. Pedagogically honest for "databases up close."
- ✅ **Deterministic, not flaky:** transactions are interleaved *sequentially* by the test (no thread races), so the
  write-skew conflict (SQLState `40001`) and the pool timeout reproduce every run.
- ✅ **No production-schema pollution:** lab tables live only inside the test run; CIF still ships exactly its real
  schema. Tests still run as part of `./mvnw verify` (they stay green forever as regression proofs).
- ✅ Reuses the already-present `testcontainers-postgresql` test dependency and the pinned image — no new deps.
- ⚠️ Raw JDBC means manual `commit`/`rollback`/`close`; mitigated with try-with-resources and the `DbLab` helpers.
- ⚠️ Read replicas / streaming replication are **not** executed here (a single container) — taught as concept +
  verify-adjacent SQL (`pg_stat_replication`, `pg_last_wal_replay_lsn()`) per §12.8, to be revisited if a replica
  topology is stood up later.
