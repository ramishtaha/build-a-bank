#!/usr/bin/env bash
# steps/step-16/smoke.sh — proves the Step-16 Spring Security work (no Docker needed):
# the security filter chain (401 without a token, 403 for the wrong role, 200 with the right one),
# JWT issue + validate (login mints a token, /me reads the identity from it), and BCrypt password hashing.
# Run from the repo root:  bash steps/step-16/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> Build + test the auth service (filter chain, JWT, BCrypt, authn/authz)"
$MVNW -B -q -pl services/auth test

echo "✅ Step 16 smoke test PASSED"
