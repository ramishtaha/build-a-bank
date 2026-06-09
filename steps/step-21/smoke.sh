#!/usr/bin/env bash
# steps/step-21/smoke.sh — proves the Step-21 payments work (needs Docker: Testcontainers Postgres + Redis + Redpanda):
#   PaymentSagaTest      : Saga happy path + COMPENSATION on a failed step + Redis Idempotency-Key (pay once)
#   PaymentControllerTest: the secured /api/v1/payments endpoint forwards the Idempotency-Key
#   DeadLetterTest       : a poison Kafka message is retried then routed to the DLT; good messages still flow
# Run from the repo root:  bash steps/step-21/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 21 needs it (Testcontainers Postgres + Redis + Redpanda). Start Docker and retry."
  exit 1
fi

echo "==> Build + test the payment Saga (compensation), Redis idempotency, the payments endpoint, and the Kafka DLQ"
$MVNW -B -q -pl services/demand-account,services/notification test \
  -Dtest='PaymentSagaTest,PaymentControllerTest,DeadLetterTest'

echo "✅ Step 21 smoke test PASSED"
