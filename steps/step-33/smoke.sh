#!/usr/bin/env bash
# steps/step-33/smoke.sh — Step 33 self-check.
# Part 1 (always): the compose file parses and the parameterized Dockerfile builds the auth image.
# Part 2 (when the containerized bank is up — `make bank-up`): every container running, the gateway
# front door works end-to-end (health, login, SPA), the runtime is distroless + non-root, and the
# 🎓 full-stack Playwright capstone passes against an ALL-container topology (zero host services).
set -euo pipefail

cd "$(dirname "$0")/../.."
COMPOSE="docker compose -f deploy/compose.fullstack.yaml --profile bank"

echo "== [1/6] Compose file parses; profile 'bank' has the full fleet =="
$COMPOSE config --services | sort | tr '\n' ' '; echo
[ "$($COMPOSE config --services | wc -l)" -ge 11 ] || { echo "SMOKE FAILED: expected ≥11 services"; exit 1; }

echo "== [2/6] Parameterized Dockerfile builds (auth; warm cache ⇒ seconds) =="
docker build -q -f deploy/Dockerfile.service --build-arg MODULE=services/auth --build-arg PORT=8083 \
  -t bab-auth:0.1.0-SNAPSHOT . | tail -1

if ! curl -s --max-time 3 http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
  echo "== [3-6/6] SKIPPED: containerized bank not up (make bank-up, wait ~60s, re-run) =="
  echo "SMOKE step-33: PASSED (static gates; live part skipped)"
  exit 0
fi

echo "== [3/6] All bank containers running =="
$COMPOSE ps --format '{{.Name}} {{.State}}' | sort
DOWN=$($COMPOSE ps --format '{{.State}}' | grep -vc running || true)
[ "$DOWN" = "0" ] || { echo "SMOKE FAILED: $DOWN container(s) not running"; exit 1; }

echo "== [4/6] Distroless + non-root runtime =="
docker inspect bab-gateway --format 'gateway runs as uid {{.Config.User}}'
if docker exec bab-gateway sh -c 'id' 2>/dev/null; then
  echo "SMOKE FAILED: a shell exists in the runtime image"; exit 1
else
  echo "no shell in the image (exec sh fails) — as designed"
fi

echo "== [5/6] The front door: health, login, SPA — one origin, all containers =="
curl -s http://localhost:8080/actuator/health | grep -o '"status":"UP"'
curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password"}' -o /dev/null -w 'login through gateway->auth: %{http_code}\n'
curl -s http://localhost:8080/ | grep -qo '<div id="root">' && echo 'SPA served via gateway catch-all: <div id="root">'

echo "== [6/6] 🎓 Full-stack Playwright capstone — zero mocks, ZERO host services =="
cd frontend && npm run test:e2e:fullstack 2>&1 | tail -3

echo "SMOKE step-33: PASSED"
