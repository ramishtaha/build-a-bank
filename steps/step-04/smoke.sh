#!/usr/bin/env bash
# steps/step-04/smoke.sh — proves the Step-4 JVM lab builds, tests, and the demos run.
# Run from the repo root:  bash steps/step-04/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"
CP="playground/java-basics/target/classes"

echo "==> 1/3 Build + test (JvmLabTest)"
$MVNW -B -q -pl playground/java-basics -am verify

echo "==> 2/3 BytecodeSample runs and computes correctly"
java -cp "$CP" com.buildabank.basics.jvm.BytecodeSample | grep -q "sumTo(100)  = 5050"

echo "==> 3/3 AllocationDemo runs under a small heap (forces GC)"
java -Xmx64m -cp "$CP" com.buildabank.basics.jvm.AllocationDemo 1000000 | grep -q "checksum="

echo "✅ Step 4 smoke test PASSED"
