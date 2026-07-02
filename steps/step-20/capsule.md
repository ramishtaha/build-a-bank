# 🧳 Capsule — Step 20

**Exists now:** 11 modules build green (`./mvnw verify` BUILD SUCCESS). Services + ports: hello=8080, cif=8081, demand-account=8082, auth=8083, **notification=8084 (NEW, no DB)**, plus gateway. Kafka topic `transfers.completed` (Redpanda `v24.2.7` via Testcontainers `@ServiceConnection`). Tests: demand-account **37** (34 prior + 3 new), notification **3**. Notification endpoints: `GET /api/notifications/stream` (SSE) + `GET /api/notifications` (recent buffer).

**This step added:**
- `TransferCompletedEvent` (record; `eventId` = end-to-end dedupe key) + `TransferEventListener` (`@TransactionalEventListener(AFTER_COMMIT)` — log/metric only, publish never lives there)
- Transactional Outbox: `V3__outbox.sql` (`outbox_event` + partial index on unpublished), `OutboxEvent`/`OutboxEventRepository`/`OutboxWriter`; written in `TransferService.post()` in the same tx as the ledger
- `OutboxRelay` (poll oldest-first, `send(topic, eventId, payload).get()`, then `markPublished`) + `OutboxRelayScheduler` (every 2s, `bank.outbox.relay.scheduled` toggle)
- NEW `services/notification`: idempotent `@KafkaListener` (dedupe by `eventId` in `ConcurrentHashMap.newKeySet()`) + `SseHub` (CopyOnWriteArrayList emitters, 50-deep recent buffer) + `NotificationController`
- Tests: `OutboxWriteTest` (commit → 1 row, rollback → 0), `OutboxRelayKafkaTest` (real Redpanda; 2nd run publishes 0), `TransferEventConsumerKafkaTest` (3 delivered / 2 applied), `NotificationControllerTest`; §12.3 mutation (dedupe disabled → `expected: 2 but was: 3`), reverted; smoke.sh PASSED; ADR-0011

**Gotchas:**
- Boot 4: use `spring-boot-starter-kafka` (+ `-test`) — bare `spring-kafka` compiles but gives **no `KafkaTemplate` bean**
- notification parses with Jackson 3 (`tools.jackson`); demand-account's OutboxWriter serializes with its own Jackson 2 mapper — same JSON on the wire; `amount` travels as a JSON number (decimal-string/minor units deferred to Step 21)
- tests must set `bank.outbox.relay.scheduled=false` and call `OutboxRelay.publishPending()` directly (shipped in `src/test/resources/application.properties`)
- consumer dedupe set is in-memory (restart forgets processed ids); notification has no auth yet (risk R-002, like cif)

**Callback hooks:**
- at-least-once delivery + dedupe by stable `eventId` = exactly-once **effect** — Step 21 (Saga, Idempotency Key in Redis, DLQ) builds directly on this pipeline
- polling relay is the teaching version; Step 54 replaces it with CDC (Debezium) / EOS
- the Kafka message key IS the event id (partition ordering + dedupe key)

**Next step starts:** `step-20-end == step-21-start`; green: `./mvnw verify` (11 modules), all 40 tests, `steps/step-20/smoke.sh`.
