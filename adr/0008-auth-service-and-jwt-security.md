# ADR-0008: A dedicated Auth service; stateless JWT security (HMAC for now), BCrypt passwords

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 16 — Spring Security deep I

## Context
Step 16 introduces Spring Security. Several choices, each with real consequences for the rest of the platform.

1. **Where to apply security first?** Secure an existing service (e.g. demand-account) in place, or stand up the
   dedicated **Identity & Auth** service the architecture (Part III) already lists?
2. **Session vs token?** Server sessions (cookies) or stateless JWTs?
3. **JWT signing:** symmetric (HMAC) or asymmetric (RSA/EC, JWK)?
4. **User store:** DB-backed now, or in-memory?

## Decision

### A dedicated `services/auth` service (not securing demand-account in place)
Build the security deep-dive in a **new `services/auth` module**. Rationale:
- It matches the architecture's service list (Identity & Auth is its own service).
- Securing demand-account in place would force authentication into **all 27 of its existing tests** at once —
  a huge, noisy change that buries the security concepts. A focused auth service teaches the filter chain,
  JWT, BCrypt, and authn/authz cleanly. **Step 17** then makes demand-account/cif/the gateway *resource
  servers* that validate this service's tokens (the natural next move).

### Stateless JWT (no server session)
Configure `SessionCreationPolicy.STATELESS` and secure endpoints as an **OAuth2 resource server** validating
`Authorization: Bearer <jwt>`. Rationale: stateless scales horizontally (no sticky sessions/shared session store),
fits an API/microservices platform, and is what the gateway + multiple services need. **CSRF is disabled** —
correctly: CSRF attacks ride ambient cookie/session credentials, and a stateless Bearer-token API has none. (If
we later add cookie-based browser sessions, CSRF protection comes back on for those.)

### HMAC (HS256) signing for now → asymmetric later
Sign/validate JWTs with a shared **HMAC secret** (HS256) via Nimbus (`NimbusJwtEncoder`/`NimbusJwtDecoder`).
Simple and self-contained for one issuer+validator. The trade-off: every validator needs the *secret* (can
forge tokens). When multiple independent services validate tokens (Step 17+), switch to **asymmetric** signing
(RSA/EC): the auth service holds the private key, services validate with the public key via a **JWKS** endpoint —
validators can't mint tokens. Flagged for Step 17 / the Authorization Server (Step 41).

### In-memory BCrypt user store
An in-memory map seeded with **BCrypt**-hashed demo passwords (fake creds only). Rationale: Step 16 is about the
*security mechanics*, not persistence; a DB-backed store + OIDC + MFA arrive in Step 17+. Passwords are never
stored/compared in plaintext (BCrypt: slow-by-design, per-hash salt, constant-time `matches`).

## Consequences
- ✅ A clean, self-contained Spring Security deep-dive (filter chain, authn vs authz, JWT, BCrypt, headers) with
  9 tests and a real login→token→authorized-call flow — no disruption to existing services/tests.
- ✅ Verified Spring Security 7 + OAuth2 resource server + Nimbus JWT (HS256) + BCrypt **resolve and work on
  Boot 4.0.6**. Filter-chain DSL: `SecurityFilterChain` bean + lambdas (`WebSecurityConfigurerAdapter` removed in
  Security 6; `authorizeHttpRequests`/`requestMatchers`).
- ⚠️ HMAC means validators share the secret (can forge). Move to asymmetric (JWKS) when other services validate
  tokens — **Step 17 / 41**.
- ⚠️ In-memory users + a demo secret are **not production** — DB users, Vault-managed keys (Phase H), rotation.
- 📝 Spring Security 7 grants authentication-**factor** authorities (e.g. `FACTOR_BEARER`) alongside roles; the
  `/me` endpoint filters to `ROLE_*` so the API contract reports roles, not internal factors.
- 🔁 Step 17: resource-server validation in the money services + the gateway, OAuth2/OIDC, MFA/passkeys, step-up.
