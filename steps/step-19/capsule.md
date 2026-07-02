# 🧳 Capsule - Step 19

**Exists now:** full repo = 10 modules, `./mvnw verify` BUILD SUCCESS; bank services unchanged (no HTTP surface this step). New `playground/distributed-lab` (pure JVM — no Docker, no Spring):
- 5 classes: `LamportClock`, `VectorClock`, `QuorumSystem`, `DeliverySim`, `ReplicatedRegister`
- 13 tests: `LogicalClockTest` 3 · `QuorumTest` 3 · `DeliverySemanticsTest` 4 · `CapPacelcTest` 3

**This step added:**
- parent `pom.xml` `<modules>` entry + module pom (junit-jupiter + assertj, test scope, versions from parent BOM)
- clocks/: Lamport (respects causality, can't detect concurrency) + vector clock (happens-before AND concurrency)
- quorum/: `everyWriteAndReadQuorumIntersect` brute-forces ALL subsets == `W+R>N` (N=5); strict (3,2,2) reads latest, sloppy (3,1,1) reads stale
- delivery/: naive ×3 → 300 (overcount) vs idempotent ×3 → 100 (exactly-once effect); 0× → loss
- cap/: CP partition → `Unavailable` thrown, replicas agree; AP partition → divergent reads (200/300) → `heal()` → both 300 (LWW); PACELC-else async → peer stale until `sync()`
- `steps/step-19/smoke.sh` + `make play-19`; §12.3 mutation (drop CP guard) → `CapPacelcTest:25` FAILURE → reverted

**Gotchas:**
- labs are in-process simulations (§12.8 honesty note), not a real cluster — real multi-node behavior arrives with Step 20+ infra
- vector-clock snapshots must be immutable; `happensBefore` = "≤ in every component AND < in at least one"
- `heal()` must call `sync()`; LWW timestamps are caller-supplied (deterministic), tie-break = timestamp then replica id
- `QuorumSystem` uses `List.removeLast()` (Java 21+; module inherits compiler release 25); no new ADR (reuses Step-11 lab idiom)

**Callback hooks:**
- exactly-once *effect* = idempotent consumer over at-least-once delivery (formalizes Step 14's Idempotency-Key) → held against Kafka in Steps 20-21
- `W+R>N` quorum dial → Kafka replication & acks (Step 20); CAP framing: money path CP-flavored, a "last seen balance" widget could be AP

**Next step starts:** `step-19-end == step-20-start` (clean-room §12.4 confirmed). Green: `./mvnw verify` (10 modules) + 13 lab tests + smoke.sh PASSED. Step 20 = Spring events + Kafka (Redpanda) + Outbox.
