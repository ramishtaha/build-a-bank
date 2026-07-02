#!/usr/bin/env bash
# steps/step-31/smoke.sh — Step 31 self-check: frontend quality gates (MSW tests, lint, build, Playwright E2E).
# Run from the repo root: bash steps/step-31/smoke.sh
set -euo pipefail

cd "$(dirname "$0")/../../frontend"

echo "== [1/4] Vitest (MSW network-level mocks, a11y, i18n) =="
npx vitest run --reporter=dot 2>&1 | tail -4

echo "== [2/4] ESLint =="
npm run lint --silent 2>&1 | tail -2 || { echo "LINT FAILED"; exit 1; }

echo "== [3/4] Production build (tsc + vite) =="
npm run build --silent 2>&1 | tail -2

echo "== [4/4] Playwright E2E (real Chromium, hermetic route mocks) =="
npx playwright test 2>&1 | tail -3

echo "SMOKE step-31: PASSED"
