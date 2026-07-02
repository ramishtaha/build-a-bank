# 🧳 Capsule - Step 21

**Exists now:** 11 modules green (`./mvnw verify` BUILD SUCCESS). demand-account **42** tests (37 prior + 5 new), notification **4** tests (3 prior + 1 new). Kafka pipeline (Step 20) + payment Saga + Redis + DLT live. Endpoint: `POST /api/v1/payments` (secured; optional `Idempotency-Key` header; unauth → 401). Redis `redis:7.4-alpine` (tests via Testcontainers; local demo on 6379).

**This step added:**
- `PaymentService` (Saga orchestrator, NOT `@Transactional`) + `PaymentStepService` (debit/credit/refund, each `Propagation.REQUIRES_NEW`) with refund **compensation**; `PaymentFailedException` → 422 in `GlobalExceptionHandler`.
- `RedisIdempotencyStore` (`SET key paymentId NX EX ttl` via `setIfAbsent`) — same `Idempotency-Key` pays once, returns the original `paymentId`.
- `POST /api/v1/payments` + `PaymentRequest`/`PaymentResponse` DTOs (`PaymentControllerTest`: forwards the key, unauth → 401).
- notification: `KafkaErrorHandlingConfig` — `DefaultErrorHandler(FixedBackOff(0, 2))` + `DeadLetterPublishingRecoverer` → `transfers.completed.DLT`; consumer no longer swallows exceptions.
- Tests: `PaymentSagaTest` (3, real Postgres + Redis), `PaymentControllerTest` (2), `DeadLetterTest` (1, real Redpanda). ADR-0012.

**Gotchas:**
- Same-config `@SpringBootTest` classes share a cached Spring context (same consumer bean/counters/Redpanda) — assert **deltas**, not totals.
- If the consumer catches exceptions, nothing ever reaches the DLT — poison must propagate to the error handler.
- Idempotency is record-after-success: safe for **sequential** retries only (concurrent-duplicate and crash-before-record windows remain; reserve-then-complete is a Your-Turn).
- Redis tests need `@ServiceConnection(name = "redis")` on the container + `spring-boot-starter-data-redis` on the classpath.
- BOLA gap R-001 still open on the payment endpoint (no `from`-ownership check) — tracked, not fixed.

**Callback hooks:**
- Step 22 reuses the Redis added here (caching, `@Cacheable`, CQRS read model).
- Saga + Outbox + idempotency revisited at Step 24 (Phase-D capstone) and Step 52 (event sourcing).
- §12.3 mutation proof: removing the refund fails `PaymentSagaTest…:75 expected: 100.00 but was: 60.0000` (reverted).

**Next step starts:** `step-21-end == step-22-start` — 11 modules, `./mvnw verify` green, `smoke.sh` PASSED, tagged `step-21-end`.
