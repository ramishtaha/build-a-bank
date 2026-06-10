#!/usr/bin/env bash
# steps/step-24/smoke.sh — proves the Step-24 batch work + the Phase-D capstone (needs Docker: Postgres + Redpanda):
#   InterestAccrualJobTest          : the EOD interest-accrual batch job — chunk read/process/write, FILTER a
#                                     zero-balance account, SKIP a sentinel (fault tolerance), step counts asserted
#   PaymentExactlyOnceCapstoneTest  : 🎓 payment end-to-end — Idempotency-Key + Outbox→Kafka + a forced duplicate
#                                     redelivery → applied exactly once (exactly-once EFFECT)
# Run from the repo root:  bash steps/step-24/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 24 needs it (Testcontainers Postgres + Redpanda). Start Docker and retry."
  exit 1
fi

echo "==> Run the EOD interest-accrual batch job + the Phase-D exactly-once capstone"
$MVNW -B -q -pl services/demand-account test \
  -Dtest='InterestAccrualJobTest,PaymentExactlyOnceCapstoneTest'

echo "✅ Step 24 smoke test PASSED — End of Phase D 🎖️"
