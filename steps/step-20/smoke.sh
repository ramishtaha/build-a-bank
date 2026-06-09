#!/usr/bin/env bash
# steps/step-20/smoke.sh — proves the Step-20 event-driven pipeline (needs Docker: Testcontainers Postgres + Redpanda):
#   demand-account  : @TransactionalEventListener(AFTER_COMMIT) + transactional OUTBOX + relay → Kafka
#   notification    : idempotent @KafkaListener (exactly-once *effect*) → SSE real-time push
# Run from the repo root:  bash steps/step-20/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 20 needs it (Testcontainers Postgres + Redpanda). Start Docker and retry."
  exit 1
fi

echo "==> Build + test the producer side (Outbox + relay → Kafka) and the consumer side (idempotent consumer + SSE)"
$MVNW -B -q -pl services/demand-account,services/notification test \
  -Dtest='OutboxWriteTest,OutboxRelayKafkaTest,TransferEventConsumerKafkaTest,NotificationControllerTest'

echo "✅ Step 20 smoke test PASSED"
