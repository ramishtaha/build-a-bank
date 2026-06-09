# ADR-0011: Event-driven notifications — Spring events + the Outbox pattern + Kafka + a notification service

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 20 — Spring events + Kafka (Redpanda) + Notification + real-time push; Phase D

## Context
Phase D makes the bank distributed. The first real piece: when a transfer commits in demand-account, other
parts of the system should react (notify the customer, feed read models) **without** demand-account calling
them synchronously. That means **asynchronous, decoupled events**. Three design questions: (1) how to react
to a committed transaction in-process; (2) how to get the event onto Kafka **reliably** despite the
dual-write problem; (3) where the consumer + real-time push live. This applies the Step-19 theory
(at-least-once delivery, exactly-once *effect* via idempotency) for real.

## Decision

### 1. In-process domain events via `@TransactionalEventListener(AFTER_COMMIT)`
`TransferService` publishes a `TransferCompletedEvent` through `ApplicationEventPublisher` inside the transfer
transaction. A `@TransactionalEventListener(phase = AFTER_COMMIT)` reacts **only after the transaction
commits** — so we never react to money that rolled back (a plain `@EventListener` fires immediately, even on
a doomed transaction). This listener does only safe in-process work (a metric/log).

### 2. The Outbox pattern for reliable hand-off to Kafka (NOT publishing from the listener)
Publishing to Kafka from the AFTER_COMMIT listener would be a **dual-write**: if the app dies between the DB
commit and the Kafka send, the event is lost forever. Instead, `TransferService` writes an **`outbox_event`
row in the same transaction** as the ledger change (atomic — both commit or neither). A polling
**`OutboxRelay`** drains unpublished rows, publishes each to Kafka (keyed by event id), and marks them
published **only after a successful send** → **at-least-once** delivery. The relay runs on a schedule in
production (`OutboxRelayScheduler`, `@EnableScheduling`), and is invoked directly in tests for determinism
(`bank.outbox.relay.scheduled=false` via a test-only `application.properties` that *merges over* — not
replaces — the main YAML). A production system would publish outside the DB transaction and reconcile, or use
CDC (Debezium, Step 54); polling keeps the pattern visible and testable here.

### 3. A new `services/notification` (no DB): idempotent consumer + SSE push
The consumer side is its own service (the curriculum's "Notification"), with **no database** — its state is
the live stream. A `@KafkaListener` consumes `transfers.completed`, **dedupes by `eventId`** (in-memory set
→ exactly-once *effect* over at-least-once delivery), and pushes each notification to connected browsers via
**Server-Sent Events** (`SseEmitter`, `text/event-stream`) — simpler than WebSocket for one-way server→client
push. The dedupe store is in-memory for teaching; Step 21 makes it durable (Idempotency Key in Redis).

### 4. Kafka via Redpanda, verified with Testcontainers + `@ServiceConnection`
Redpanda (Kafka-API compatible, lighter) is the broker, pinned `redpandadata/redpanda:v24.2.7` (digest in
VERSIONS.md). Tests use `RedpandaContainer` + Spring Boot `@ServiceConnection` (auto-wires
`spring.kafka.*`) — real broker, no hand-rolled config. The producer side (Outbox→Kafka) and consumer side
(dedupe→SSE) are each proven against a real broker.

### 5. Jackson: producer serializes with Jackson 2, consumer parses with Jackson 3
demand-account already owns a `com.fasterxml` (Jackson 2) mapper for JSON (Step 14). The new notification
service's web starter brings **Jackson 3** (`tools.jackson`, Boot 4's default), with no Jackson-2 on its
classpath — so its consumer injects the Boot-autoconfigured Jackson 3 `ObjectMapper`. The wire format is
standard JSON, so cross-version interop is a non-issue; this is just "use what each module's classpath has."

## Consequences
- ✅ demand-account and notification are **decoupled** — demand-account doesn't know notification exists; new
  consumers can be added without touching the producer.
- ✅ **No lost events on crash** (Outbox atomicity) and **no double-effect on redelivery** (idempotent
  consumer) — the Step-19 guarantees, realized. Proven end-to-end on a real broker.
- ✅ A real **user-facing feature**: open `GET /api/notifications/stream` and watch transfers appear live.
- ⚠️ Polling relay adds latency (≤ poll interval) and holds the DB tx during the blocking send — acceptable
  for teaching; flagged for CDC/transactional-producer upgrade (Step 54).
- ⚠️ Consumer dedupe is **in-memory** → resets on restart (a restart could reprocess). Durable idempotency
  (Redis) is Step 21; noted honestly.
- ⚠️ The notification service has **no auth yet** (consistent with cif, R-002) and SSE has no backpressure/
  auth — fine for the local demo; revisit with the gateway/security work.
- 🔁 Step 21 (Saga + Idempotency Key in Redis + DLQ), Step 22 (caching/CQRS read model), Step 54 (Schema
  Registry, EOS, CDC). The notification SSE stream is the first surface the Phase-F React app will consume.
