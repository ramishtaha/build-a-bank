#!/usr/bin/env bash
# steps/step-07/smoke.sh — proves the Step-7 AOP aspect + the Phase-A capstone vertical slice.
# Run from the repo root:  bash steps/step-07/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> 1/3 Build + test (incl. MockMvc slice + self-invocation proof)"
$MVNW -B -q -pl playground/spring-lab -am verify

echo "==> 2/3 Boot the capstone app"
JAR="$(ls playground/spring-lab/target/spring-lab-*.jar | grep -v original | head -n1)"
java -jar "$JAR" --server.port=8082 > /tmp/bab-step07.log 2>&1 &
APP=$!; trap 'kill "$APP" 2>/dev/null || true' EXIT
for i in $(seq 1 40); do curl -fs http://localhost:8082/api/accounts/1 >/dev/null 2>&1 && break; sleep 1; [ "$i" = "40" ] && { echo "!! app not up"; tail -n 20 /tmp/bab-step07.log; exit 1; }; done

echo "==> 3/3 Hit the slice + assert (200 body, 404, and the aspect logged)"
curl -fs http://localhost:8082/api/accounts/1 | grep -q "Ada Lovelace" || { echo "!! account body wrong"; exit 1; }
code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/api/accounts/999)"; [ "$code" = "404" ] || { echo "!! expected 404 got $code"; exit 1; }
grep -q "AUDIT" /tmp/bab-step07.log || { echo "!! audit aspect did not log"; exit 1; }

echo "✅ Step 7 smoke test PASSED"
