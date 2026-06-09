#!/usr/bin/env bash
# steps/step-14/smoke.sh — proves the Step-14 API-design work on a real Postgres (Testcontainers):
# URI versioning (/api/v1) + deprecation headers on the old path, public-API idempotency (Idempotency-Key
# → money moves once on retry), pagination (PageResponse envelope), and signed outbound webhooks
# (HMAC-SHA256 + timestamp replay protection + bounded retries, verified by an in-test receiver).
# Run from the repo root:  bash steps/step-14/smoke.sh   (needs Docker)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 14 needs it (Testcontainers Postgres). Start Docker and retry."
  exit 1
fi

echo "==> Build + test demand-account (versioning, idempotency, pagination, signed webhooks) on real Postgres"
$MVNW -B -q -pl services/demand-account -am verify

echo "✅ Step 14 smoke test PASSED"
