#!/usr/bin/env bash
# steps/step-13/smoke.sh — proves the Step-13 Spring MVC / REST work on a real Postgres (Testcontainers):
# RFC 9457 ProblemDetail error handling, validation Problem Details, the RequestIdFilter + TimingInterceptor
# headers, and a LIVE OpenAPI doc (/v3/api-docs) + Swagger UI (/swagger-ui) served over real HTTP.
# Run from the repo root:  bash steps/step-13/smoke.sh   (needs Docker)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 13 needs it (Testcontainers Postgres). Start Docker and retry."
  exit 1
fi

echo "==> Build + test demand-account (ProblemDetail, OpenAPI/Swagger, filter/interceptor) on real Postgres"
$MVNW -B -q -pl services/demand-account -am verify

echo "✅ Step 13 smoke test PASSED"
