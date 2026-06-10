#!/usr/bin/env bash
# steps/step-25/smoke.sh — proves the Step-25 SOLID refactor of the notification consumer is BEHAVIOUR-PRESERVING:
#   • the UNCHANGED integration tests still pass (the refactor safety net) — TransferEventConsumerKafkaTest,
#     DeadLetterTest, NotificationControllerTest  (needs Docker: Testcontainers Redpanda)
#   • the extracted collaborators are now unit-testable on their own — TransferEventParserTest,
#     InMemoryProcessedEventStoreTest  (no Docker)
# Run from the repo root:  bash steps/step-25/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — the notification integration tests need it (Testcontainers Redpanda). Start Docker and retry."
  exit 1
fi

echo "==> Re-run the notification suite: same behaviour (integration tests unchanged) + new unit tests for the extracted parts"
$MVNW -B -q -pl services/notification test

echo "✅ Step 25 smoke test PASSED — refactor preserved behaviour (SRP + DIP applied)"
