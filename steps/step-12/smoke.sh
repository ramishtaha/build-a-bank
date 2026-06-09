#!/usr/bin/env bash
# steps/step-12/smoke.sh — proves the Step-12 Demand Account work on a real Postgres (Testcontainers):
# the double-entry transfer + balanced ledger pair, rollback on overdraw, REQUIRES_NEW propagation,
# optimistic @Version locking, a live HTTP round-trip, and the Phase-B capstone — a concurrency stress
# test that FAILS without locking (lost update) and PASSES with pessimistic locking.
# Run from the repo root:  bash steps/step-12/smoke.sh   (needs Docker)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 12 needs it (Testcontainers Postgres). Start Docker and retry."
  exit 1
fi

echo "==> Build + test the Demand Account service (ledger, transactions, locking, capstone) on real Postgres"
$MVNW -B -q -pl services/demand-account -am verify

echo "✅ Step 12 smoke test PASSED"
