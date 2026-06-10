#!/usr/bin/env bash
# steps/step-22/smoke.sh — proves the Step-22 caching/async/scheduling work (needs Docker: Testcontainers Redis):
#   MarketCacheTest      : @Cacheable read model (upstream hit once per pair), @CachePut refresh, @Async on a virtual thread
#   ShedLockTest         : the ShedLock Redis lock — a held lock blocks a second acquire (the cluster guard)
#   MarketControllerTest : the read endpoint returns the rate
# Run from the repo root:  bash steps/step-22/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 22 needs it (Testcontainers Redis). Start Docker and retry."
  exit 1
fi

echo "==> Build + test the FX cache read model, @Async on virtual threads, and ShedLock clustered scheduling"
$MVNW -B -q -pl services/market-info test \
  -Dtest='MarketCacheTest,ShedLockTest,MarketControllerTest'

echo "✅ Step 22 smoke test PASSED"
