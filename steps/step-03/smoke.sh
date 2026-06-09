#!/usr/bin/env bash
# steps/step-03/smoke.sh — proves the Step-3 networking lab builds and its loopback HTTP tests pass.
# The LoopbackHttpTest starts a real JDK HttpServer on an ephemeral port and exercises both the
# java.net.http client AND a raw socket against it — a self-contained end-to-end HTTP round trip.
# Run from the repo root:  bash steps/step-03/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> Build + test the networking lab (loopback HTTP round-trip + URL anatomy)"
$MVNW -B -q -pl playground/java-basics -am verify

echo "==> Compile-check the demos are runnable (class present)"
test -f playground/java-basics/target/classes/com/buildabank/basics/net/HttpClientDemo.class
test -f playground/java-basics/target/classes/com/buildabank/basics/net/RawHttpDemo.class

echo "✅ Step 3 smoke test PASSED (networking lab green)"
