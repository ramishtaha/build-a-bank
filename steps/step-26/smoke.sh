#!/usr/bin/env bash
# steps/step-26/smoke.sh — proves the Step-26 HEXAGONAL restructure of the notification service is
# behaviour-preserving (needs Docker: Testcontainers Redpanda):
#   • the domain/application/adapter layering compiles and wires up;
#   • the integration tests still pass with ONLY their imports changed (classes moved packages) — the proof
#     that a structural move didn't change behaviour: TransferEventConsumerKafkaTest, DeadLetterTest,
#     NotificationControllerTest;
#   • the core's unit tests still pass: TransferEventParserTest, InMemoryProcessedEventStoreTest.
# Run from the repo root:  bash steps/step-26/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — the notification integration tests need it (Testcontainers Redpanda). Start Docker and retry."
  exit 1
fi

echo "==> Re-run the notification suite after the hexagonal restructure (domain/application/adapter layers)"
$MVNW -B -q -pl services/notification test

echo "✅ Step 26 smoke test PASSED — hexagonal restructure preserved behaviour (ports-and-adapters)"
