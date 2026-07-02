# 🧳 Capsule — Step 26

**Exists now:** full repo builds (13 modules, `./mvnw verify` BUILD SUCCESS). `services/notification` is hexagonal: `domain` (TransferEvent, Notification — `java.*` imports only) · `application` (NotificationService + `port/in/NotifyOnTransfer`, `port/out/{ProcessedEventStore, NotificationPublisher}`) · `adapter/in/{messaging,web}` · `adapter/out/{persistence,push}`. Notification suite: 7 tests green (Testcontainers Redpanda). SSE endpoints `GET /api/notifications` + `/stream` unchanged since Step 20.

**This step added:**
- Hexagonal repackaging of all 9 notification classes (domain/application/adapter rings; behaviour preserved)
- New inbound port `NotifyOnTransfer` + new outbound port `NotificationPublisher`; Step 25's `ProcessedEventStore` moved to `application/port/out`
- New `NotificationService` use case (markIfNew → Notification.from → publish, via ports only)
- `TransferEventConsumer` rewired to drive `NotifyOnTransfer` (keeps parser call + received/applied counters)
- ADR-0017; `steps/step-26/smoke.sh` (re-runs the suite)

**Gotchas:**
- Boundaries are package convention only until Step 27's ArchUnit — a Spring import in `domain/` still compiles today.
- Deliberate coupling: web adapter (`NotificationController`) uses the push adapter (`SseHub`) directly — shared SSE transport, documented, allowed by Step 27's rules.
- Tests live in the root package, so the move only ADDED import lines; assertions untouched — that unchanged-assertion diff is the behaviour-preservation proof.
- `@Service` stays on the use case (pragmatic compromise); only the domain is strictly annotation-free.
- `InMemoryProcessedEventStore` dedup resets on restart (in-memory Set).

**Callback hooks:**
- The dependency rule (inward) + per-layer allowed-imports list → encoded by ArchUnit/Spring Modulith in Step 27.
- §12.3 mutation: use case ignoring `markIfNew` → `TransferEventConsumerKafkaTest…:67 expected: 2 but was: 3`, reverted — exactly-once runs through the application layer.
- Proportionate DDD call (value objects + application service, no aggregates in a thin context) — contrasted with the money domain later in Phase E.

**Next step starts:** `step-26-end == step-27-start`. Green: `./mvnw verify` (13 modules), notification 7 tests (imports-only diff), `smoke.sh` PASSED, tag `step-26-end`.
