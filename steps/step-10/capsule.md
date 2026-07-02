# 🧳 Capsule - Step 10

**Exists now:** `services/cif` green with **21 tests** (10 from Steps 8–9 + 11 raw-JDBC lab tests) on Testcontainers Postgres 17.10 (`postgres:17-alpine` pinned, Testcontainers 2.0.5, random high JDBC port). No `src/main` changes, no new HTTP endpoints — the Step-8 CIF API is unchanged. New test package `services/cif/src/test/java/com/buildabank/cif/dblab/` + `steps/step-10/{queries.sql,smoke.sh}`.

**This step added:**
- `DbLab` harness — singleton Testcontainers Postgres + raw-JDBC helpers (`openTx`/`openAuto`/`explain`/`exec`/`scalar`)
- `QueryPlanLabTest` — Seq Scan (`Rows Removed by Filter: 19996`) → Bitmap Index Scan → Index Only Scan (`Heap Fetches: 0`)
- `MvccIsolationTest` — no dirty read in PG; non-repeatable @RC (100→200); stable snapshot @RR; phantom @RC (2→3) not @RR
- `WriteSkewTest` — sum −100 @REPEATABLE READ vs SQLSTATE 40001 @SERIALIZABLE (sum held at 50)
- `ConnectionPoolTest` — HikariCP size-2 pool: 3rd borrow timed out ~506 ms, recovered on return
- `PartitioningLabTest` — pruning: only `txn_2026_02` scanned, Jan/Mar never in the plan
- `OnlineSchemaChangeTest` — CREATE INDEX CONCURRENTLY → 25001 inside a txn, builds in autocommit; fast default backfills 1000 rows
- ADR-0004; `queries.sql` (hand-run psql experiments) + `smoke.sh`

**Gotchas:**
- Testcontainers 2.0: `PostgreSQLContainer` is non-generic (the old `<SELF>` self-type was dropped)
- `CREATE INDEX CONCURRENTLY` refuses to run inside any transaction (25001) — autocommit only; on failure it can leave an INVALID index (check `pg_index.indisvalid`)
- After a 40001 the connection's txn is aborted: `rollback()` first, then retry the whole logical operation
- `EXPLAIN ANALYZE` executes the statement — it mutates on UPDATE/DELETE
- Planner may pick Bitmap Index Scan (not plain Index Scan) even for ~4 rows — assert node names, read the plan; `Heap Fetches > 0` if VACUUM hasn't set the visibility map
- Read replicas are concept-only: a single Testcontainers node cannot demonstrate streaming replication (§12.8 honesty note)

**Callback hooks:** Step 11 = the in-JVM half of the same concurrency question (JMM); Step 12 picks the money-movement defense (SERIALIZABLE + retry-on-40001 vs `SELECT … FOR UPDATE` vs `@Version`) and uses expand-contract migrations for real; Step 36 graphs the Hikari MXBean gauges (`threadsAwaitingConnection`) as production alerts.

**Next step starts:** `step-10-end` == `step-11-start` (same commit, confirmed). Green: `./mvnw -pl services/cif -am verify` (21 tests, BUILD SUCCESS), `steps/step-10/smoke.sh` PASSED, §12.3 mutation check done & reverted (`WriteSkewTest.java:89`), clean-room fresh-clone verified.
