# ADR-0012: Payments as a Saga (orchestration) + Redis Idempotency-Key + Kafka retries/DLQ

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 21 — Payments: cross-account transfers; Phase D

## Context
Step 12's transfer moves money inside **one** database transaction — atomic, with rollback on failure. But a
real payment often spans steps that **can't share a transaction** (different services/shards, an external
settlement). Once a step commits, you can't roll it back; you must **compensate**. Step 20 gave us the Kafka
substrate and an in-memory idempotency dedupe. Step 21 makes payments reliable across step boundaries:
**Saga** for cross-step consistency, a **durable idempotency key** so retries don't double-pay, and a
**dead-letter** path so a poison message doesn't wedge the consumer.

## Decision

### 1. Model the payment as an orchestration Saga with explicit compensation
A new `PaymentService` orchestrates discrete steps in `PaymentStepService`, each `@Transactional(REQUIRES_NEW)`
so it **commits independently**: `debit(source)` → `credit(dest)`; if `credit` fails after `debit` committed,
the orchestrator runs the **compensation** `refund(source)` and throws `PaymentFailedException`. We chose
**orchestration** (one coordinator) over **choreography** (services react to events with no coordinator)
because the flow is short and a single coordinator is easiest to reason about and test; the lesson contrasts
both, and the Step-20 Kafka pipeline is the substrate a choreographed version would use. The steps live in a
separate bean because `REQUIRES_NEW` only applies through the Spring proxy (self-invocation pitfall, Step 7).

> This deliberately does NOT replace the Step-12 single-transaction transfer (still the right tool when one
> ACID transaction is available). The Saga is for when it isn't — we model that with independently-committed
> steps so compensation is genuinely required (and proven by a test: a missing destination → debit committed →
> refund → source balance restored, no money lost or created).

### 2. Durable Idempotency-Key in Redis
`RedisIdempotencyStore` records `Idempotency-Key → paymentId` with `SET … NX EX` (`setIfAbsent` + 24h TTL). A
retried payment with the same key returns the original `paymentId` without paying again. **Why Redis** over
the DB store (Step 14) or the in-memory set (Step 20): fast, shared across instances, and auto-expiring (keys
only matter for a retry window). Sequential-retry correctness is covered; the concurrent-double-submit nuance
(reserve-then-complete) is noted for a hardening pass.

### 3. Retries + Dead-Letter Topic on the consumer
The notification consumer no longer swallows exceptions. A `DefaultErrorHandler` with a
`DeadLetterPublishingRecoverer` retries a failing message a few times (`FixedBackOff(0, 2)`) and then
republishes it to `<topic>.DLT` (an explicit destination resolver, so the name is deterministic). A poison
(un-parseable) message is quarantined on `transfers.completed.DLT` for inspection while good messages keep
flowing — instead of being silently dropped (the old swallow-and-log) or blocking the partition forever.

### 4. Infrastructure: Redis via Testcontainers `@ServiceConnection`
`spring-boot-starter-data-redis` (Lettuce). Tests use a `GenericContainer` of `redis:7.4-alpine` (pinned, see
VERSIONS.md) with `@ServiceConnection(name = "redis")`, which auto-wires `spring.data.redis.*`. Kafka stays on
Redpanda (Step 20).

## Consequences
- ✅ A multi-step payment is **consistent under partial failure** — compensation restores balances; proven on
  real Postgres + Redis.
- ✅ Payments are **safe to retry** (durable Redis idempotency) — money moves once per key.
- ✅ A poison message can't wedge the consumer — it lands on a **DLT**; proven on a real broker.
- ✅ Reuses the proven resource-server security (the payment endpoint is authenticated like every money endpoint).
- ⚠️ The Saga is **orchestration**; a choreographed, event-sourced version (and Saga across *separate services*)
  is a later evolution (Phase J event sourcing, Step 52). Compensation here is a refund, not a full reversal
  workflow.
- ⚠️ Redis idempotency handles sequential retries; concurrent duplicate submits want a reserve-then-complete
  protocol — flagged.
- ⚠️ The DLT has no automated re-drive/alerting yet (manual inspection) — observability/alerting is Phase G/H.
- 🔁 Step 22 (caching + CQRS read model on Redis), Step 24 (batch — EOD reconciliation), Step 52 (event
  sourcing / full CQRS), Step 54 (Schema Registry, EOS, CDC). 🎓 Phase-D capstone (Step 24 horizon): a payment
  end-to-end across Kafka with Outbox + Saga + idempotency, exactly-once *effect* under a forced retry.
