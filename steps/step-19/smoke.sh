#!/usr/bin/env bash
# steps/step-19/smoke.sh — proves the Step-19 distributed-systems theory labs (pure JVM, NO Docker needed):
# logical/vector clocks (causality + concurrency detection), quorums (W+R>N intersection), delivery
# semantics (exactly-once *effect* via idempotency), and CAP/PACELC (CP vs AP + reconcile).
# Run from the repo root:  bash steps/step-19/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> Run the Step-19 distributed-systems labs (pure JVM, no infrastructure)"
$MVNW -B -q -pl playground/distributed-lab test \
  -Dtest='LogicalClockTest,QuorumTest,DeliverySemanticsTest,CapPacelcTest'

echo "✅ Step 19 smoke test PASSED"
