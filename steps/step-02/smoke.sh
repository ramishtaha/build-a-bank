#!/usr/bin/env bash
# steps/step-02/smoke.sh — proves the Step-2 Java primer builds, tests, and the demo runs.
# Run from the repo root:  bash steps/step-02/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> 1/2 Build + test java-basics (and its parent)"
$MVNW -B -q -pl playground/java-basics -am verify

echo "==> 2/2 Run the Step 2 demo and assert key output"
OUT="$(java -cp playground/java-basics/target/classes com.buildabank.basics.Step2Demo)"
echo "$OUT"
echo "$OUT" | grep -q "net movement   : 1124.50 USD" || { echo "!! unexpected demo output"; exit 1; }
echo "$OUT" | grep -q "3.25% APR" || { echo "!! pattern-match describe output missing"; exit 1; }

echo "✅ Step 2 smoke test PASSED"
