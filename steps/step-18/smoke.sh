#!/usr/bin/env bash
# steps/step-18/smoke.sh — proves the Step-18 secure-coding work:
#   • cif queries are injection-safe (parameterized) — SqlInjectionSafetyTest (with a vulnerable contrast)
#   • demand-account ships secure-by-default headers + deny-by-default CORS — SecurityHardeningTest
#   • the threat model + risk register exist and name the BOLA finding (R-001)
# Run from the repo root:  bash steps/step-18/smoke.sh   (needs Docker for cif/demand-account Testcontainers)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running — Step 18's cif/demand-account tests need it (Testcontainers). Start Docker and retry."
  exit 1
fi

echo "==> 1/3  Threat-model artifacts present and naming the BOLA finding"
test -f security/threat-model.md  || { echo "missing security/threat-model.md"; exit 1; }
test -f security/risk-register.md || { echo "missing security/risk-register.md"; exit 1; }
grep -q "R-001" security/risk-register.md || { echo "risk register must track R-001 (BOLA)"; exit 1; }
grep -qi "BOLA"  security/threat-model.md || { echo "threat model must discuss BOLA"; exit 1; }
echo "   ✓ threat-model.md + risk-register.md present; BOLA / R-001 tracked"

echo "==> 2/3  Injection-safety (cif) + edge hardening (demand-account) tests"
$MVNW -B -q -pl services/cif,services/demand-account -am \
  test -Dtest='SqlInjectionSafetyTest,SecurityHardeningTest'

echo "==> 3/3  Secure defaults are actually wired in SecurityConfig"
grep -q "frameOptions"            services/demand-account/src/main/java/com/buildabank/account/web/SecurityConfig.java
grep -q "corsConfigurationSource" services/demand-account/src/main/java/com/buildabank/account/web/SecurityConfig.java
echo "   ✓ headers + deny-by-default CORS present"

echo "✅ Step 18 smoke test PASSED"
