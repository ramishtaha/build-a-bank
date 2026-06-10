#!/usr/bin/env bash
# steps/step-23/smoke.sh — proves the Step-23 onboarding orchestration:
#   OnboardingOrchestrationTest : create customer → open account over real HTTP (in-process stubs); on a
#                                 forced account-open failure the orchestrator COMPENSATES (deactivate);
#                                 the bearer token is forwarded downstream. (No Docker needed.)
#   OnboardingControllerTest    : POST /api/onboarding → 201 + result; invalid body → 400
#   cif CustomerControllerTest  : the new POST /api/customers/{id}/deactivate compensation endpoint → 204
# Run from the repo root:  bash steps/step-23/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> Test the onboarding orchestrator + compensation (no Docker) and the CIF deactivate endpoint"
$MVNW -B -q -pl services/onboarding test
$MVNW -B -q -pl services/cif test -Dtest='CustomerControllerTest'

echo "✅ Step 23 smoke test PASSED"
