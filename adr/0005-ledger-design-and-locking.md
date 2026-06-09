# ADR-0005: Double-entry ledger with a materialized balance, pessimistic locking for transfers

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 12 — Demand Account + double-entry ledger + transaction management

## Context
The Demand Account service must move money between accounts correctly under concurrent load. Several design
axes had to be settled, and they are exam-grade interview topics, so the choices are recorded here.

1. **Balance representation.** Store a materialized `balance` on `account`, or derive it by summing ledger entries?
2. **Concurrency control for transfers.** Optimistic (`@Version`, Step 9), pessimistic (`SELECT … FOR UPDATE`,
   Step 10), or SERIALIZABLE + retry?
3. **Money type.** `BigDecimal` vs integer minor units vs `double`.
4. **How to demonstrate the failure** (the Phase-B capstone needs to *fail without locking*) given `@Version` is
   always on the entity and would itself prevent silent corruption.

## Decision
- **Double-entry ledger + materialized balance.** Every transfer appends two immutable `ledger_entry` rows (a DEBIT
  and a CREDIT sharing a `transaction_id`) *and* updates the two accounts' `balance`, all in one `@Transactional`
  transaction. The ledger is the append-only audit trail (always nets to zero); the balance is the fast-read
  projection kept in sync transactionally. (Full event sourcing is deferred to Step 52.)
- **Pessimistic locking is the default for money movement.** `transfer` loads both accounts with
  `@Lock(PESSIMISTIC_WRITE)` (`SELECT … FOR UPDATE`), locking in a **deterministic account-number order** to avoid
  deadlock. Rationale: a hot account is the high-contention, must-serialize case; optimistic locking storms with
  retries there (measured: 17 of 20 concurrent transfers fail with only `@Version`). Optimistic `@Version` remains on
  `Account` and is taught/tested (`OptimisticLockTest`) as the alternative for low-contention edits (e.g. KYC, Step 9).
- **Money = `BigDecimal`** (`numeric(19,4)`), compared with `compareTo`; **time = UTC `Instant`** (`timestamptz`).
  Never `double`. ("Minor units" is taught as the concept; the stored type is exact decimal, per the Operating
  Contract.)
- **Capstone failure demonstration** uses a clearly-labelled `transferUnsafe` (a bulk absolute write that bypasses
  both the lock *and* `@Version`) plus a `CyclicBarrier` test seam, so the lost update is **deterministic** (not
  flaky). The safe path (`transfer`) is contrasted under real 20-thread contention.

## Consequences
- ✅ Correct, auditable money movement; the books always balance (`ledgerNet == 0`), money is conserved, and no
  account overdraws — all asserted under concurrent load.
- ✅ Both locking strategies are demonstrated and contrasted with real numbers — strong interview material.
- ✅ `REQUIRES_NEW` audit (`AuditService`) shows propagation; lives in a separate bean to dodge the self-invocation
  pitfall (Step 7).
- ⚠️ The materialized balance must be kept in lock-step with the ledger; a bug that writes one without the other
  corrupts state — mitigated by doing both in one transaction and asserting reconciliation in tests.
- ⚠️ Pessimistic locks reduce concurrency on hot rows (serialized) and can deadlock if lock order isn't disciplined —
  mitigated by the account-number lock ordering.
- 🔁 Revisit at Step 21 (Saga + idempotency for cross-service transfers) and Step 52 (event-source the ledger;
  balances become projections/read models).
