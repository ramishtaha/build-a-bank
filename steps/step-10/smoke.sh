#!/usr/bin/env bash
# steps/step-10/smoke.sh — proves the Step-10 "relational databases up close" work:
# the six raw-JDBC labs on a real Testcontainers Postgres — query plans (seq → index → index-only),
# MVCC & isolation anomalies, write skew (REPEATABLE READ breaks it, SERIALIZABLE rejects it),
# the HikariCP pool timeout, partition pruning, and online schema change.
# Run from the repo root:  bash steps/step-10/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 10 needs it (Testcontainers Postgres). Start Docker and retry."
  exit 1
fi

echo "==> Run the six Step-10 database labs on a real Postgres (Testcontainers)"
$MVNW -B -q -pl services/cif test \
  -Dtest='QueryPlanLabTest,MvccIsolationTest,WriteSkewTest,ConnectionPoolTest,PartitioningLabTest,OnlineSchemaChangeTest'

echo "✅ Step 10 smoke test PASSED"
