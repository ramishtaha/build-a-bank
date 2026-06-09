#!/usr/bin/env bash
# steps/step-15/smoke.sh — proves the Step-15 gateway + service-to-service work:
# the gateway (Spring Cloud Gateway Server WebMVC) routes to a downstream, strips the prefix, and adds a
# response header (verified against an in-test stub); and the declarative @HttpExchange CifClient
# deserializes a response and trips its read timeout on a slow dependency.
# Run from the repo root:  bash steps/step-15/smoke.sh   (needs Docker for the demand-account Testcontainers tests)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 15's demand-account tests need it (Testcontainers). Start Docker and retry."
  exit 1
fi

echo "==> Build + test the gateway (routing) and demand-account (incl. the CifClient s2s call)"
$MVNW -B -q -pl gateway,services/demand-account -am verify

echo "✅ Step 15 smoke test PASSED"
