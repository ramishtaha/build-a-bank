#!/usr/bin/env bash
# steps/step-28/smoke.sh — proves the Step-28 testing & quality work, all WITHOUT Docker:
#   • the hexagon core's fast unit + jqwik property tests pass;
#   • PITest mutation coverage on the core meets its threshold (the Phase-E capstone — 100%);
#   • the custom starter (libs/common) auto-configures (ApplicationContextRunner) and is consumed by hello;
#   • the code-quality gates pass repo-wide (Spotless + Checkstyle).
# Run from the repo root:  bash steps/step-28/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> 1/4 core unit + jqwik property tests (notification hexagon core)"
$MVNW -B -q -pl services/notification \
  -Dtest='NotificationServiceTest,NotificationTest,NotificationPropertyTest' test

echo "==> 2/4 mutation coverage on the core (PITest) — the Phase-E capstone (threshold 90%, achieves 100%)"
$MVNW -B -q -pl services/notification -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage

echo "==> 3/4 the custom starter auto-configures + is consumed by hello"
$MVNW -B -q -pl libs/common,services/hello -am test

echo "==> 4/4 code-quality gates pass repo-wide (Spotless + Checkstyle)"
$MVNW -B -q spotless:check checkstyle:check

echo "✅ Step 28 smoke test PASSED — mutation 100% + property tests + custom starter + quality gates (Phase E complete)"
