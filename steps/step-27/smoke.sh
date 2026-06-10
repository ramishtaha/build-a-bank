#!/usr/bin/env bash
# steps/step-27/smoke.sh — proves the Step-27 architecture fitness functions are in place and green.
# Both are STATIC bytecode analysis — they start no Spring context and need NO Docker:
#   • notification HexagonalArchitectureTest — 4 ArchUnit rules enforce the Step-26 hexagon (domain pure;
#     application transport-agnostic + adapter-free; dependencies point inward).
#   • demand-account ModularityTest — Spring Modulith ApplicationModules.verify() (no module cycles, no
#     access to another module's internals) + Documenter living docs into target/spring-modulith-docs.
# Run from the repo root:  bash steps/step-27/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> ArchUnit: enforce the notification hexagon (domain pure, application transport-agnostic, arrows inward)"
$MVNW -B -q -pl services/notification -Dtest=HexagonalArchitectureTest test

echo "==> Spring Modulith: verify demand-account's modules have no cycles + generate living docs"
$MVNW -B -q -pl services/demand-account -Dtest=ModularityTest test

DOCS="services/demand-account/target/spring-modulith-docs"
if [ -f "$DOCS/components.puml" ]; then
  echo "==> Living docs generated: $(ls "$DOCS"/module-*.adoc 2>/dev/null | wc -l | tr -d ' ') module canvases + components.puml"
else
  echo "!! expected generated module docs under $DOCS — Documenter did not run"; exit 1
fi

echo "✅ Step 27 smoke test PASSED — architecture enforced (ArchUnit hexagon + Spring Modulith modules)"
