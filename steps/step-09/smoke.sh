#!/usr/bin/env bash
# steps/step-09/smoke.sh — proves the Hibernate performance/correctness work:
# the N+1 statistics proof (3 statements lazy vs 1 with @EntityGraph), the projection, and the
# @Version optimistic-locking conflict — all on a real Postgres via Testcontainers (needs Docker).
# Run from the repo root:  bash steps/step-09/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 9 needs it (Testcontainers Postgres). Start Docker and retry."
  exit 1
fi

echo "==> Build + test CIF (N+1 statistics + optimistic locking + projection, on real Postgres)"
$MVNW -B -q -pl services/cif -am verify

echo "✅ Step 9 smoke test PASSED"
