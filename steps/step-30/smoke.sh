#!/usr/bin/env bash
# steps/step-30/smoke.sh — proves the Step-30 frontend data/forms/SSE work + the gateway notification route.
# SPA build/lint/test need Node (NOT Docker); the gateway routing test needs neither Docker nor a browser.
#   • the SPA type-checks and builds (tsc + vite);
#   • ESLint passes;
#   • Vitest + Testing Library suite passes (TanStack Query account panel, RHF+Zod transfer form, SSE hook);
#   • the gateway routes /notifications/** (and auth/cif/demand-account) — the single front door.
# Run from the repo root:  bash steps/step-30/smoke.sh
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

echo "==> 4/4 gateway fronts notifications (+ auth/cif/demand-account) — the single front door (no Docker)"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"
$MVNW -B -q -pl gateway -Dtest=GatewayRoutingTest test

echo "✅ Step 30 smoke test PASSED — TanStack Query + RHF/Zod transfer + SSE live updates on the gateway front door"
