#!/usr/bin/env bash
# steps/step-05/smoke.sh — proves the Step-5 Spring lab builds, tests, and the IoC demo runs.
# Run from the repo root:  bash steps/step-05/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> 1/2 Build + test spring-lab"
$MVNW -B -q -pl playground/spring-lab -am verify

echo "==> 2/2 Run the app and assert the IoC demo output"
JAR="$(ls playground/spring-lab/target/spring-lab-*.jar | grep -v original | head -n1)"
OUT="$(java -jar "$JAR" 2>&1)"
echo "$OUT" | grep -q "wired RateProvider     : fixed" || { echo "!! provider not wired as expected"; exit 1; }
echo "$OUT" | grep -q "singleton same instance? true" || { echo "!! singleton scope wrong"; exit 1; }
echo "$OUT" | grep -q "(same? false)" || { echo "!! prototype scope wrong"; exit 1; }

echo "✅ Step 5 smoke test PASSED"
