# ADR-0016: SOLID refactor of the notification consumer — extract collaborators + a DIP port

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 25 — SOLID & Clean-Code Principles (refactor a smelly service); opens Phase E

## Context
Phase E is design/architecture/testing mastery. Step 25 picks a service with genuine smells and refactors it
toward **SOLID**, keeping behaviour identical (the existing tests are the safety net). The clearest target was
`notification`'s `TransferEventConsumer`: a single `@KafkaListener` method that **counted, parsed JSON, deduped,
built the notification + message, published, and logged** — a textbook **SRP** violation — and depended directly
on a concrete `ObjectMapper` and an inline `ConcurrentHashMap` — a **DIP** violation. This also sets the
direction for Step 26 (hexagonal) without doing the full restructure yet.

## Decision

### 1. Behaviour-preserving refactor — the tests don't change
The refactor must not change observable behaviour, so the existing tests (`TransferEventConsumerKafkaTest`,
`DeadLetterTest`, `NotificationControllerTest`) are **left untouched** and must stay green — that's the proof a
refactor is safe. (Refactor = change structure, not behaviour.)

### 2. SRP — extract single-responsibility collaborators
- `TransferEvent` — a plain domain record (no JSON/Kafka coupling).
- `TransferEventParser` — the parsing concern (isolates Jackson); a poison payload **throws** here (preserving
  the Dead-Letter routing from Step 21).
- `Notification.from(TransferEvent)` — message-wording in one place, derived from the domain event (not a
  `JsonNode`), removing the "reach into JSON" feature-envy.
- `TransferEventConsumer` becomes a **thin orchestration**: parse → dedupe → notify (one line each).

### 3. DIP — a `ProcessedEventStore` port + an in-memory adapter
The idempotency mechanism is now a **port** (`ProcessedEventStore.markIfNew`) with an
`InMemoryProcessedEventStore` adapter. The consumer depends on the abstraction, not a `ConcurrentHashMap`, so a
durable adapter (Redis, as in Step 21) can replace it with **no consumer change** — Dependency Inversion + the
Open/Closed Principle. This is the ports-and-adapters seed Step 26 will grow into a full hexagon.

### 4. Testability is the payoff
Because the parser and the store are now separate and depend on abstractions, each is unit-testable without
Kafka or a Spring context (`TransferEventParserTest`, `InMemoryProcessedEventStoreTest`) — a concrete benefit
of SOLID, not just aesthetics.

## Consequences
- ✅ The consumer is small and reads as the workflow; each collaborator has one reason to change.
- ✅ DIP: idempotency is swappable behind a port (Redis-ready) — no consumer change needed.
- ✅ New, fast unit tests for the extracted pieces; the unchanged integration tests prove behaviour preserved.
- ✅ Sets up Step 26 (hexagonal/ports-and-adapters) and the Phase-E capstone.
- ⚠️ This is a *partial* application of ports-and-adapters (one port), deliberately — full hexagonal layering
  (domain/application/adapters packages, an inbound port for the listener) is Step 26.
- ⚠️ Only `notification` was refactored; other services have their own smells to address as they're touched
  (e.g. demand-account's `TransferService` breadth) — not a blanket rewrite.
- 🔁 Step 26 (hexagonal + DDD), Step 27 (Spring Modulith + ArchUnit to *enforce* these boundaries), Step 28
  (code-quality gates), Phase-E capstone (hexagonal + ArchUnit + mutation testing).
