# 🧳 Capsule — Step 18

**Exists now:** 9 Maven modules, `./mvnw verify` green. Services: gateway :8080 · cif :8081 (Postgres, **24** tests) · demand-account :8082 (OAuth2 resource server, **34** tests) · auth :8083 (RS256 + JWKS). `security/threat-model.md` + `security/risk-register.md` versioned in-repo; ADRs through 0010.

**This step added:**
- `security/threat-model.md` — STRIDE-per-element over a DFD, 5 trust boundaries, 2 attack trees, OWASP Top 10 (2021) + API Top 10 (2023) walkthroughs, v1-transfer capstone
- `security/risk-register.md` — **R-001 BOLA (CRITICAL, OPEN)** on transfer/balance/entries: recorded & scheduled, deliberately not half-fixed (§12.8)
- demand-account `SecurityConfig`: explicit headers (nosniff / DENY / no-referrer / HSTS) + **deny-by-default CORS** (`app.security.cors.allowed-origins`, empty ⇒ deny all)
- `SecurityHardeningTest` (+3 → 34): headers on every response; un-listed-origin preflight → 403, no ACAO; unauth → 401
- `SqlInjectionSafetyTest` (+3 → 24, Testcontainers): parameterized matches **0** vs concat contrast **1**; destructive payload → table survives
- ADR-0010 (threat model + secure defaults)

**Gotchas:**
- CORS source must resolve **by NAME** via `.cors(Customizer.withDefaults())` — injecting `CorsConfigurationSource` by type fails the context (2 beans: `corsConfigurationSource` + `mvcHandlerMappingIntrospector`)
- HSTS is only emitted over TLS — never assert it in plain-HTTP tests
- BOLA (R-001) is intentionally open — no test claims it is fixed

**Callback hooks:**
- R-001 fix is specified (add `owner_subject` to `account`, set from `jwt.getSubject()`, enforce ownership → 403) and owned by a dedicated authorization step in Phase D/E
- The React app is allow-listed later via `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173` (Step 29)
- Re-threat-model when Phase D (Kafka) adds new elements and trust boundaries

**Next step starts:** `step-18-end == step-19-start` — **END OF PHASE C**. Green: full build (9 modules), both new test classes, §12.3 CORS mutation failed-then-reverted, `smoke.sh` PASSED.
