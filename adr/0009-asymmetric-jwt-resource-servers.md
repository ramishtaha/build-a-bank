# ADR-0009: Asymmetric (RS256) JWTs + JWKS; the money services become resource servers; method security

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 17 — Spring Security deep II + modern auth

## Context
Step 16 stood up the auth service with **HMAC (HS256)** JWTs — fine for one issuer that also validates. Step 17
makes *other* services (starting with demand-account) validate those tokens. With a shared HMAC secret, every
validator would also be able to **forge** tokens — unacceptable as the platform grows. We also need fine-grained,
domain-level authorization beyond URL rules, and a credible story for MFA/step-up.

## Decision

### 1. Asymmetric signing (RS256) + a published JWKS
The auth service now signs with an **RSA private key (RS256)** and publishes the matching **public key** at
`GET /oauth2/jwks` (a JWK Set). Resource servers validate with the public key — they can verify but **cannot mint**
(least privilege). The key pair is **generated at startup** (ephemeral; validators fetch the current key from JWKS).
*Trade-off:* a restart rotates the key (invalidating live tokens) — fine for the course; production persists the key
in a keystore/Vault (Phase H) and rotates deliberately (JWKS supports multiple keys for rollover).

### 2. demand-account becomes an OAuth2 resource server
demand-account validates incoming `Bearer` JWTs via `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` →
auth's JWKS (lazy fetch, so it boots even if auth is down). Money endpoints (`/api/**`) require authentication;
health and the API docs stay public. **Verified live:** demand-account fetched auth's JWKS and accepted an
auth-minted token (201) while rejecting an unauthenticated request (401).

### 3. URL authorization at the edge + `@PreAuthorize` method security for fine-grained rules
Coarse "all money endpoints require a valid token" is a URL rule (`authorizeHttpRequests`). Fine-grained,
domain-level rules use **`@EnableMethodSecurity` + `@PreAuthorize`** (e.g. admin-only operations). **Blast-radius
choice:** we deliberately do NOT put `@PreAuthorize` on the core transfer *service* methods — that would force a
security context into the dozens of service-level unit tests that call them directly. Instead the HTTP edge is
secured by URL rules, and method security is demonstrated on a narrow admin endpoint. So only the **HTTP-layer
tests** (the controller slice + the integration test) needed auth, not the service-level ones.

### 4. Testing without committing private keys
- Controller slice (`@WebMvcTest`): `spring-security-test`'s `jwt()` request post-processor injects the
  authentication directly (no real token/decoder). Needed `@EnableWebSecurity` on the config so the slice gets an
  `HttpSecurity` bean, plus a mock `JwtDecoder`.
- Integration test (`@SpringBootTest`, real HTTP): a `@TestConfiguration` supplies a `JwtDecoder` built from a
  **test** RSA public key (overriding the production `jwk-set-uri`), and the test mints RS256 tokens with the
  matching private key. **No private keys are committed.**

## Consequences
- ✅ Multiple services validate tokens without holding a signing secret — the correct trust model for microservices.
- ✅ Verified end-to-end live (cross-service JWKS) AND per-side in tests (auth 11 tests; demand-account 31 tests).
- ✅ Method security demonstrated without breaking the service-level test suite (URL rules at the edge).
- ⚠️ Ephemeral key → restart invalidates tokens; **persist + rotate via keystore/Vault in Phase H** (JWKS handles
  multi-key rollover).
- ⚠️ The automated tests use a *test* key (offline); the real auth↔demand-account JWKS interop is proven by a
  **live cross-service run** (documented in the Verification Log per §12.8), not by the unit suite.
- 📝 **MFA / passkeys (WebAuthn) / step-up** are taught as a *taste* this step: the mechanism for step-up is
  SS7 authentication-factor authorities + `@PreAuthorize`; the full WebAuthn credential ceremony needs the
  frontend (Phase F) and is flagged, not faked.
- 🔁 Step 41 (Authorization Server / Keycloak), Phase H (Vault-managed keys, mTLS), Phase F (WebAuthn UI).
