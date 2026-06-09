#!/usr/bin/env bash
# steps/step-06/smoke.sh — proves Step-6 type-safe config + the custom auto-configuration work.
# Run from the repo root:  bash steps/step-06/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> 1/2 Build + test spring-lab (incl. BankPropertiesTest + GreetingAutoConfigurationTest)"
$MVNW -B -q -pl playground/spring-lab -am verify

echo "==> 2/2 Run the app; assert typed @ConfigurationProperties + the auto-configured GreetingService"
JAR="$(ls playground/spring-lab/target/spring-lab-*.jar | grep -v original | head -n1)"
OUT="$(java -jar "$JAR" 2>&1)"
echo "$OUT" | grep -q "greeting (auto-config) : Welcome to Build-a-Bank, intern!" || { echo "!! auto-config greeting missing"; exit 1; }
echo "$OUT" | grep -q "annual rate (props)    : 3.25%" || { echo "!! typed properties not bound"; exit 1; }

echo "✅ Step 6 smoke test PASSED"
