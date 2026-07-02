# 🧳 Capsule - Step 12

**Exists now:** 7 Maven modules; services: hello (8080), cif (8081), **demand-account** (8082, own Postgres — compose maps host **5433**). demand-account: `Account` (BigDecimal + `@Version`), append-only `LedgerEntry` (double-entry), `AuditEntry`; `TransferService` (pessimistic `transfer` + demo-only `transferUnsafe`), `AuditService` (REQUIRES_NEW), `PropagationDemoService`; REST: POST /api/accounts (201), POST /api/transfers (200), GET /api/accounts/{n} (200/404), errors 422 `insufficient_funds` / 400. **11 tests** in 6 classes (ConcurrentTransferTest 2, TransferControllerTest 4, TransferServiceTest 2, OptimisticLockTest 1, TransactionPropagationTest 1, DemandAccountIntegrationTest 1).

**This step added:**
- NEW module `services/demand-account` (the 7th) — accounts + double-entry ledger; Flyway V1 (`account`, `ledger_entry`, `audit_log`); database-per-service.
- Pessimistic `SELECT … FOR UPDATE` transfer (`@Lock(PESSIMISTIC_WRITE)`) with deadlock-safe lock ordering by account number; two ledger legs share one `transactionId`.
- `@Transactional` deep: REQUIRES_NEW audit survives outer rollback (proven); default rollback on RuntimeException only; `readOnly` reads.
- 🎓 Phase-B capstone: no-lock lost update made deterministic via CyclicBarrier (A=100/B=100); with the lock 20/20 concurrent transfers succeed (A=0/B=1000, total 1000, ledgerNet 0). ADR-0005.

**Gotchas:**
- Tests share one Postgres and `@SpringBootTest` doesn't roll back — delete `ledger_entry` BEFORE `account` in `@BeforeEach` (FK order).
- Live run uses host port **5433** (local 5432 is taken): `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/demand_account`; the inline env prefix is bash-only (PowerShell: `$env:` + `.\mvnw.cmd`).
- `@Lock` only works inside a transaction; REQUIRES_NEW only across a bean boundary (self-invocation bypasses the proxy).
- Removing FOR UPDATE ≠ lost money: `@Version` still conserves the total, but **17/20 transfers get rejected** with optimistic-lock failures (§12.3 mutation, reverted).

**Callback hooks:**
- The measured contrast — 17/20 optimistic failures without FOR UPDATE vs 20/20 success with it — is the course's canonical locking trade-off datum.
- Step 21 (Payments) builds Saga + idempotency on these transfers; Step 52 event-sources the ledger; Step 22 adds distributed locks.
- Error mapping here is a minimal Map body; RFC-9457 `ProblemDetail` arrives in Step 13.

**Next step starts:** `step-12-end` == `step-13-start` (🎖️ end of Phase B). Green: full-repo `./mvnw verify` BUILD SUCCESS (7 modules), demand-account 11/11 incl. capstone both ways, live HTTP 201/201/200/422, `smoke.sh` PASSED, clean-room verified. Step 13 = Spring MVC / REST deep.
