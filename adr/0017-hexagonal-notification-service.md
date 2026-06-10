# ADR-0017: Restructure the notification service to hexagonal (ports-and-adapters) + DDD value objects

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 26 — Clean / hexagonal architecture + DDD; Phase E

## Context
Step 25 applied SOLID and introduced one port (`ProcessedEventStore`) in the notification service. Step 26
completes the move to **hexagonal architecture (ports-and-adapters)**: a framework-free core with the outside
world (Kafka, SSE, in-memory store) attached only through ports. This both improves the design and gives
Step 27's **ArchUnit** concrete, package-based boundaries to enforce.

## Decision

### 1. Package the notification service as a hexagon
```
com.buildabank.notification
├── domain/                      TransferEvent, Notification        (value objects; no framework/transport)
├── application/                 NotificationService                (the use case)
│   ├── port/in/                 NotifyOnTransfer                   (INBOUND/driving port)
│   └── port/out/                ProcessedEventStore, NotificationPublisher  (OUTBOUND/driven ports)
├── adapter/in/                  (driving adapters)
│   ├── messaging/               TransferEventConsumer (@KafkaListener), TransferEventParser, KafkaErrorHandlingConfig
│   └── web/                     NotificationController             (SSE endpoints)
└── adapter/out/                 (driven adapters)
    ├── persistence/             InMemoryProcessedEventStore        (implements ProcessedEventStore)
    └── push/                    SseHub                             (implements NotificationPublisher)
```
**Dependency rule:** dependencies point **inward** — adapters → application ports → domain; the domain and
application depend on nothing in the adapter ring. `domain` has no Spring/Kafka/Jackson imports at all.

### 2. The use case behind an inbound port
`NotifyOnTransfer.handle(TransferEvent) → boolean` is the inbound port; `NotificationService` implements it,
orchestrating only through outbound ports (`ProcessedEventStore`, `NotificationPublisher`). The Kafka listener
(driving adapter) parses the wire payload and calls the port — it holds no business logic. Transport (Jackson,
Kafka, SSE) lives entirely in adapters.

### 3. DDD tactical — value objects + an application service (right-sized)
`TransferEvent` and `Notification` are immutable **value objects**; `NotificationService` is an **application
service**. The notification context is a thin read/push context, so it has **no aggregates/repositories** — and
that's the correct call: DDD tactical patterns are applied where they earn their place, not ceremonially.
(Richer aggregates live in the money domain — demand-account.)

### 4. Behaviour-preserving — only test imports change
No behaviour changed, so the integration tests' **assertions are untouched**; only their `import` lines update
because classes moved packages. Those unchanged assertions passing is the proof the restructure is safe. Two
small accommodations: the SSE `NotificationPublisher` push adapter (`SseHub`) is also what the web adapter uses
to serve the subscribe/recent endpoints (the SSE transport is shared in↔out) — a documented coupling; and the
consumer's received/applied counters stay on the listener edge as an observability seam.

## Consequences
- ✅ A clean hexagon: a framework-free core, swappable adapters (Redis store, WebSocket push) with no core change.
- ✅ Concrete package boundaries for **ArchUnit** to enforce in Step 27.
- ✅ Behaviour preserved — the unchanged integration tests + the Step-25 unit tests still pass.
- ⚠️ One pragmatic coupling: the web adapter uses the SSE push adapter directly (shared SSE transport) — Step 27's
  ArchUnit rules will allow this explicitly while still forbidding any adapter→core-inward violation.
- ⚠️ Only `notification` is hexagonal; demand-account/cif remain layered-but-not-hexagonal (refactor them when a
  reason arises — the Phase-E capstone may deepen one).
- 🔁 Step 27 (Spring Modulith + ArchUnit — enforce these boundaries), Step 28 (code-quality gates), Phase-E
  capstone (hexagonal + ArchUnit + mutation testing).
