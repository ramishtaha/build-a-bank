#!/usr/bin/env bash
# steps/step-11/smoke.sh — proves the Step-11 concurrency work (pure JVM, NO Docker needed):
# a deterministic lost-update race + its three fixes (synchronized / AtomicLong / LongAdder),
# plus the java.util.concurrent toolkit (virtual threads, CompletableFuture, Semaphore).
# Run from the repo root:  bash steps/step-11/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> Run the Step-11 concurrency labs (pure JVM, no Docker)"
$MVNW -B -q -pl playground/concurrency-lab test \
  -Dtest='LostUpdateRaceTest,ConcurrencyToolsTest'

echo "✅ Step 11 smoke test PASSED"
