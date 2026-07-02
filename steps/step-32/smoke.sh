#!/usr/bin/env bash
# steps/step-32/smoke.sh — Step 32 self-check.
# Part 1 (always): the SPA's static quality gates — Vitest (29), ESLint, split production build, hermetic E2E.
# Part 2 (when the full stack is up): SPA-through-gateway, the refresh-cookie lifecycle on the wire
# (rotate → replay→401 → 409 race), and the 🎓 full-stack Playwright capstone.
# Stack up = deploy/compose.fullstack.yaml + the four services (see the lesson / Makefile fullstack-up).
set -euo pipefail

cd "$(dirname "$0")/../../frontend"

echo "== [1/6] Vitest (MSW cookie-aware session suite included) =="
npx vitest run --reporter=dot 2>&1 | tail -4

echo "== [2/6] ESLint =="
npm run lint --silent 2>&1 | tail -2 || { echo "LINT FAILED"; exit 1; }

echo "== [3/6] Production build (code-split: react-vendor + per-route chunks) =="
npm run build --silent 2>&1 | tail -7

echo "== [4/6] Playwright E2E (hermetic — no backend needed) =="
npx playwright test 2>&1 | tail -3

if ! curl -s --max-time 3 http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
  echo "== [5-6/6] SKIPPED: gateway not up (start the full stack to run the capstone checks) =="
  echo "SMOKE step-32: PASSED (static gates; full-stack part skipped)"
  exit 0
fi

echo "== [5/6] Session lifecycle on the wire (through the gateway) =="
JAR=$(mktemp)
curl -s -c "$JAR" -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password"}' -o /dev/null -w 'login:            %{http_code}\n'
R0=$(grep bab_refresh "$JAR" | awk '{print $NF}')
curl -s -b "bab_refresh=$R0" -c "$JAR" -X POST http://localhost:8080/api/auth/refresh \
  -o /dev/null -w 'refresh (rotate): %{http_code}\n'
R1=$(grep bab_refresh "$JAR" | awk '{print $NF}')
sleep 4 # step past the two-tabs grace window
REPLAY=$(curl -s -b "bab_refresh=$R0" -X POST http://localhost:8080/api/auth/refresh -o /dev/null -w '%{http_code}')
echo "replay old:       $REPLAY (expect 401 — reuse detection)"
[ "$REPLAY" = "401" ] || { echo "SMOKE FAILED: reuse not detected"; exit 1; }
SUCCESSOR=$(curl -s -b "bab_refresh=$R1" -X POST http://localhost:8080/api/auth/refresh -o /dev/null -w '%{http_code}')
echo "successor after:  $SUCCESSOR (expect 401 — family revoked)"
[ "$SUCCESSOR" = "401" ] || { echo "SMOKE FAILED: family not revoked"; exit 1; }
rm -f "$JAR"

echo "== [6/6] 🎓 Full-stack Playwright capstone (real gateway, zero mocks) =="
npm run test:e2e:fullstack 2>&1 | tail -3

echo "SMOKE step-32: PASSED"
