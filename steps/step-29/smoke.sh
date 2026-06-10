#!/usr/bin/env bash
# steps/step-29/smoke.sh — proves the Step-29 frontend foundations + the gateway front-door change.
# The SPA build/lint/test need Node (NOT Docker); the gateway route/CORS test needs neither Docker nor a browser.
#   • the React+TS SPA type-checks and builds (tsc + vite);
#   • ESLint (the SPA's quality gate) passes;
#   • the Vitest + Testing Library suite passes (api client, LoginPage, ProtectedRoute);
#   • the gateway routes /api/auth/** (no strip) and enforces deny-by-default CORS.
# Run from the repo root:  bash steps/step-29/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"

if ! command -v npm >/dev/null 2>&1; then
  echo "!! npm not found — the frontend needs Node.js 22 + npm. Install Node and retry."
  exit 1
fi

echo "==> 1/4 install frontend deps if needed (npm ci — exact versions from the lockfile)"
[ -d frontend/node_modules ] || npm --prefix frontend ci

echo "==> 2/4 build the SPA (TypeScript typecheck + Vite production build)"
npm --prefix frontend run build

echo "==> 3/4 lint + test the SPA (ESLint, then Vitest + Testing Library)"
npm --prefix frontend run lint
npm --prefix frontend test

echo "==> 4/4 gateway fronts auth (/api/auth/**, no strip) + deny-by-default CORS (no Docker)"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"
$MVNW -B -q -pl gateway -Dtest=GatewayRoutingTest test

echo "✅ Step 29 smoke test PASSED — React+TS SPA (login + routing + guard) on the gateway front door (auth + CORS)"
