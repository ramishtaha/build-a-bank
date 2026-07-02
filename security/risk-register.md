# 🗂️ Build-a-Bank — Security Risk Register

> The prioritized, owned output of the [threat model](threat-model.md). Each open risk has an ID, a
> severity, the STRIDE/OWASP category it maps to, the concrete fix, and **the step that will close it**.
> "Shift-left" means we record the risk the moment we find it — not after it bites in production.
>
> **Severity** = likelihood × impact, banked-money lens. **Status:** 🔴 open · 🟡 partial · ✅ closed.

## Open & tracked

### R-001 · BOLA on money endpoints — 🔴 **CRITICAL** · OPEN
- **Maps to:** OWASP API1:2023 (BOLA) · A01:2021 · STRIDE *Elevation* + *Information disclosure* on demand-account.
- **What:** `POST /api/transfers`, `POST /api/v1/transfers`, `GET /api/accounts/{n}`, and
  `GET /api/v1/accounts/{n}/entries` take an account identifier from the request and **never check the
  authenticated subject is entitled to it.** Any user with *any* valid token can move money out of, or
  read, **any** account.
- **Why still open:** a correct fix needs an **ownership model** — accounts must carry an owner
  (the CIF/subject), which is a schema change (Flyway migration) + service-layer enforcement + reworking
  the test fixtures that currently use arbitrary account numbers. That is a focused step of its own, not a
  bolt-on, and doing it badly (e.g. half-enforced) is worse than doing it deliberately.
- **Fix (specified):** add `owner_subject` to `account`; set it from `jwt.getSubject()` on open; in
  `TransferService.transfer` and the read paths, assert the caller owns `from`/the account (else 403);
  add `@PreAuthorize`/a domain check + tests (owner 200, non-owner 403, admin override). Keep the
  double-entry invariant intact.
- **Interim control:** every endpoint already requires authentication (Step 17) — so this is exploitable
  only by an *authenticated* user against *another* account, not anonymously. Documented prominently.
- **Owner / ETA:** dedicated **Authorization & ownership** step in Phase D/E (the next time we touch the
  money service's access model). Tracked here until then.

### R-002 · cif has no application-layer authn/authz — 🟡 HIGH · OPEN
- **Maps to:** A01/A05 · STRIDE *Spoofing* on cif (PII).
- **What:** cif (customer master, holds PII) has **no Spring Security**; it trusts the network boundary
  (only the gateway can reach it). Defense-in-depth says an internal service holding PII should still
  authenticate callers.
- **Fix:** make cif an OAuth2 resource server too (same JWKS pattern as demand-account, Step 17), require
  a token (and a scope/role for PII reads). Low effort, reuses Step 17.
- **Owner / ETA:** fold into the same authorization step, or the service-mesh/mTLS work in Phase G/H.

### R-003 · No rate limiting / circuit breaking at the edge — 🟡 MEDIUM · OPEN
- **Maps to:** A04/API4 · STRIDE *Denial of service*.
- **What:** gateway and services have no throttling; a flood (credential stuffing, transfer spam) is
  unmitigated beyond page-size caps.
- **Fix:** Resilience4j rate limiter + circuit breaker at the gateway and on outbound calls.
- **Owner / ETA:** **Step 37** (resilience), already on the roadmap.

### R-004 · Webhook egress SSRF / open callback — 🟡 MEDIUM · OPEN
- **Maps to:** A10/API7 · STRIDE *Information disclosure* / pivot.
- **What:** the partner webhook URL is configuration-driven; if it ever becomes tenant/partner-supplied,
  an attacker could point it at internal addresses (cloud metadata, internal services).
- **Fix:** allow-list partner hosts, block private/link-local ranges, DNS-rebinding guard, egress proxy.
- **Owner / ETA:** when webhooks become partner-configurable (Phase E onward).

### R-005 · Ephemeral JWT signing key (no rotation/persistence) — 🟡 MEDIUM · OPEN
- **Maps to:** A02/A07 · STRIDE *Spoofing*/availability.
- **What:** auth generates its RSA key at startup (Step 17 ADR-0009); a restart invalidates all live
  tokens, and there is no rotation/overlap.
- **Fix:** persist the keypair (keystore/Vault), publish multiple keys in JWKS by `kid`, rotate with
  overlap.
- **Owner / ETA:** **Phase H** (secrets management).

### R-006 · No dependency / container vulnerability scanning gate — 🟡 MEDIUM · OPEN
- **Maps to:** A06/A08.
- **What:** versions are pinned (`VERSIONS.md`) but nothing yet *fails the build* on a known-vulnerable
  dependency or base image.
- **Fix:** dependency scanning (OWASP Dependency-Check/Trivy) + image scan wired as CI gates.
- **Owner / ETA:** **Step 40** (security gates), per the roadmap.

## Closed (verified)

| ID | Risk | Closed by | Proof |
|---|---|---|---|
| C-01 | SQL injection in CIF queries | Parameterized JPA | `SqlInjectionSafetyTest` (Step 18) — payload matches 0 via repo, 1 via concat contrast |
| C-02 | Token forgery (shared HMAC secret) | RS256 + JWKS | Step 17 / ADR-0009; validators hold public key only |
| C-03 | Algorithm-confusion / `alg:none` | Nimbus decoder pinned to RS256 | Step 17 |
| C-04 | Privilege escalation to admin ops | `@PreAuthorize` method security | Step 17 — USER 403 / ADMIN 200 |
| C-05 | Webhook forgery & replay | HMAC-SHA256 + timestamp window | Step 14 — tamper/wrong-secret/stale rejected |
| C-06 | Clickjacking / MIME-sniff / referrer leak | Security headers | Step 18 — `SecurityHardeningTest` |
| C-07 | Cross-origin browser abuse | Deny-by-default CORS | Step 18 — preflight from un-listed origin → 403 |
| C-08 | Verbose error leakage | RFC 9457 ProblemDetail | Step 13 |
| C-09 | Lost-update / overdraw races | `FOR UPDATE` + `@Version` + double-entry | Step 11/12 |
| C-10 | JWT persisted in `localStorage` (XSS-exfiltratable, carried since Step 29) | Access token in memory only + rotating httpOnly refresh cookie with reuse detection (ADR-0023) | Step 32 — `RefreshFlowTest` (rotate/replay→401/family-revoke on the wire) + full-stack Playwright: reload keeps session with `localStorage.length == 0`. Residual: active-XSS-in-page can still use the session (CSP/deps = the real defense); logout can't un-sign issued access JWTs (TTL now 10 min) |
