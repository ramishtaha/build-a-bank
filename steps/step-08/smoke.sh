#!/usr/bin/env bash
# steps/step-08/smoke.sh — proves the CIF service builds and works against a REAL Postgres.
# The integration tests use Testcontainers, so this needs Docker running (no manual DB setup).
# Run from the repo root:  bash steps/step-08/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 8 needs it (Testcontainers spins up Postgres). Start Docker and retry."
  exit 1
fi

echo "==> Build + test CIF (Testcontainers Postgres + Flyway + full POST->GET integration)"
$MVNW -B -q -pl services/cif -am verify

echo "✅ Step 8 smoke test PASSED (CIF verified on a real Postgres)"
