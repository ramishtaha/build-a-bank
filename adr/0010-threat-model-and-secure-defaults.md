# ADR-0010: Threat-model-driven secure defaults; BOLA recorded as a tracked risk, not silently fixed

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 18 — Secure coding & threat modeling (DevSecOps shift-left); end of Phase C

## Context
Phase C built the web/API/security surface (Spring MVC, API design, gateway, auth, resource servers). Before
moving to distributed messaging (Phase D), we **shift security left**: model the system with STRIDE *before*
shipping more features, and turn the findings into either code or a tracked risk. The model
([`security/threat-model.md`](../security/threat-model.md)) surfaced one **critical** finding — **BOLA**
(Broken Object Level Authorization, OWASP API1:2023) on the money endpoints — plus several lower-severity gaps.
The question this ADR settles: **what do we fix now, and what do we record-and-schedule, and why?**

## Decision

### 1. Threat model is a versioned artifact, not a one-off
We keep `security/threat-model.md` (DFD, trust boundaries, STRIDE-per-element, attack trees, OWASP Top 10 +
API Security Top 10 walkthroughs) and `security/risk-register.md` (prioritized, owned, ID'd open risks) in the
repo and review them whenever a service/flow/boundary changes. Insecure Design (A04) is mitigated by *having
and maintaining* this process.

### 2. Ship the cheap, complete edge hardening now (secure-by-default)
demand-account's `SecurityConfig` gets three explicit defaults the model called for, each backed by a test:
- **Security response headers**: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
  `Referrer-Policy: no-referrer`, and HSTS (emitted over TLS). Anti-sniffing / anti-clickjacking / no URL leak.
- **Deny-by-default CORS**: a `corsConfigurationSource` bean with an **empty** allow-list — no browser origin is
  permitted unless explicitly added via `app.security.cors.allowed-origins`. A preflight from an un-listed origin
  gets `403` and no `Access-Control-Allow-Origin`. Server-to-server callers (no `Origin` header) are unaffected.
- The existing **stateless / CSRF-disabled-by-design** posture is reaffirmed and documented (CSRF is off
  *because* there are no cookies; it must return the moment cookie auth is introduced).

> **Gotcha recorded:** injecting `CorsConfigurationSource` *by type* into the `SecurityFilterChain` fails —
> Spring MVC's `mvcHandlerMappingIntrospector` also implements that interface, so there are two beans. The
> idiomatic fix is `.cors(Customizer.withDefaults())`, which resolves the bean **by the name**
> `corsConfigurationSource`. (Verbatim error + fix in the lesson's 🩺 section.)

### 3. BOLA (R-001) is recorded and scheduled, NOT bolted on in this step
The transfer and account-read endpoints take an account identifier from the request and never check the
authenticated subject is entitled to it — **any authenticated user can move or read any account.** This is the
model's #1 finding. We deliberately **do not** fix it inside a "threat modeling" step:
- A correct fix is an **ownership model** (an `owner_subject` on `account`, set from the JWT on open, enforced on
  every money/read path) — a schema migration + service-layer authorization + reworking fixtures that today use
  arbitrary account numbers. That is a focused step of its own.
- A half-enforced authz fix is *worse* than a clearly-tracked open risk: it invites a false sense of safety.
- Real shift-left output is a **prioritized risk register**, not "fix everything in one PR." So R-001 is logged
  CRITICAL with a fully specified remediation and an owning step, and its interim control (auth *is* required, so
  it's authenticated-user-only, not anonymous) is documented.

### 4. Prove injection-safety with a side-by-side contrast (and prove the proof)
`SqlInjectionSafetyTest` runs the classic `' OR '1'='1` payload through the **parameterized** Spring Data query
(matches 0 rows) and through a **hand-concatenated** native query (matches every row) — the delta demonstrates
both that we are safe *and* that the test has teeth. The destructive `'; DROP TABLE …; --` payload is shown
treated as data, with the table intact afterward.

## Consequences
- ✅ Browser-facing attack surface shrunk (clickjacking, MIME-sniff, referrer leak, cross-origin) — tested.
- ✅ Injection-safety is now an explicit, teeth-y regression test, not an assumption.
- ✅ The bank's real critical gap (BOLA) is **named, prioritized, and owned** — honest per §12.8 — instead of
  hidden behind "we did a security step."
- ⚠️ **R-001 (BOLA) remains exploitable by an authenticated user against another account** until the dedicated
  authorization step lands. This is the most important open risk in the system; it is flagged prominently in the
  threat model, risk register, and the Step 18 lesson.
- ⚠️ cif still has no app-layer auth (R-002), no rate limiting (R-003, Step 37), ephemeral signing key (R-005,
  Phase H), and no scanning gate (R-006, Step 40) — all tracked.
- 🔁 Authorization/ownership step (R-001/R-002), Step 37 (resilience/rate limiting), Step 40 (security CI gates),
  Phase H (secrets/key rotation, SSRF egress controls).
