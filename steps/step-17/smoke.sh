#!/usr/bin/env bash
# steps/step-17/smoke.sh — proves the Step-17 security work:
# auth issues RS256 JWTs + publishes JWKS + @PreAuthorize method security; demand-account is an OAuth2
# resource server (401 without a token, 403 for the wrong role, 200 with a valid one) with method security.
# Run from the repo root:  bash steps/step-17/smoke.sh   (needs Docker for demand-account's Testcontainers tests)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 17's demand-account tests need it (Testcontainers). Start Docker and retry."
  exit 1
fi

echo "==> Build + test auth (RS256 + JWKS + method security) and demand-account (resource server + method security)"
$MVNW -B -q -pl services/auth,services/demand-account -am verify

echo "✅ Step 17 smoke test PASSED"
