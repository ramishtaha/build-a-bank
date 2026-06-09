#!/usr/bin/env bash
# steps/step-01/smoke.sh — proves the Step-1 build matches the lesson.
# Run from the repo root:  bash steps/step-01/smoke.sh
# Works in Git Bash / WSL / Linux / macOS. (Windows PowerShell users: see lesson.md §🔬.)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> 1/4 Build + test (this is the gate)"
$MVNW -B -q verify

echo "==> 2/4 Boot the packaged jar"
JAR="$(ls services/hello/target/hello-service-*.jar | grep -v original | head -n1)"
java -jar "$JAR" > /tmp/bab-step01.log 2>&1 &
APP_PID=$!
trap 'kill "$APP_PID" 2>/dev/null || true' EXIT

echo "==> 3/4 Wait for health=UP"
for i in $(seq 1 40); do
  if curl -fs http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "    healthy after ~$((i*1))s"; break
  fi
  sleep 1
  [ "$i" = "40" ] && { echo "!! app never became healthy"; tail -n 30 /tmp/bab-step01.log; exit 1; }
done

echo "==> 4/4 Hit GET /api/hello and assert the welcome message"
BODY="$(curl -fs http://localhost:8080/api/hello)"
echo "    body: $BODY"
echo "$BODY" | grep -q "Welcome to Build-a-Bank" || { echo "!! unexpected body"; exit 1; }

echo "✅ Step 1 smoke test PASSED"
