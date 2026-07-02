# 🧳 Capsule — Step 25

**Exists now:** 13 modules, full-repo `./mvnw verify` BUILD SUCCESS. `notification` service: **7 tests green** — 4 unchanged integration tests across 3 classes (`TransferEventConsumerKafkaTest`, `DeadLetterTest`, `NotificationControllerTest`) + 2 new unit test classes. `TransferEventConsumer` is a thin orchestration: `parse → markIfNew → Notification.from → SseHub.publish`.

**This step added:**
- `TransferEvent` (domain record) + `TransferEventParser` — SRP: Jackson isolated; throws on poison → DLT path preserved.
- `ProcessedEventStore` **port** + `InMemoryProcessedEventStore` **adapter** — DIP; Redis-ready; `ConcurrentHashMap.newKeySet()` behind the port.
- `Notification.from(event)` factory — message wording in one place.
- `TransferEventParserTest` + `InMemoryProcessedEventStoreTest` — fast unit tests, no Kafka/Spring.
- ADR-0016; tag `step-25-end`.

**Gotchas:**
- The parser must **throw** on poison payloads; catching inside the parser kills the DLT route (`DeadLetterTest` fails).
- Exactly one `@Component` may implement the port — gate any second adapter (Redis) with a profile/`@ConditionalOnProperty`.
- The 4 integration tests are the safety net and stayed byte-identical; if one has to change, the refactor wasn't behaviour-preserving.
- No reference solution for the Redis stretch (`solutions/` holds step-01 only).

**Callback hooks:**
- `ProcessedEventStore` port = DIP "in the small" — Step 26 grows it into the full hexagon; Step 27 enforces the boundaries with ArchUnit.
- §12.3 mutation evidence: `markIfNew` forced always-true → `InMemoryProcessedEventStoreTest…:19 expected false but was true`; reverted.
- Refactor discipline for all of Phase E: structure changes only, under an unchanged green suite.

**Next step starts:** `step-25-end == step-26-start`. Green: full-repo verify (13 modules), notification 7/7 tests, `smoke.sh` PASSED. Standard tier — clean-room skipped (no critical path).
