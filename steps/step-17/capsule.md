# 🧳 Capsule - Step 17

**Exists now:** 9 Maven modules, full `./mvnw verify` green. auth (:8083) signs RS256 and publishes `/oauth2/jwks` (public-only: `kty/kid/n/e`, no `d/p/q`) — 11 tests. demand-account (:8082) is an OAuth2 resource server (`jwk-set-uri`), money endpoints authenticated — 31 tests. Gateway (Step 15) forwards `Authorization` to downstreams.

**This step added:**
- auth: HS256 → RS256 (`JwtService` two-line change); RSA key generated at startup; `JwksController` at `/oauth2/jwks` (permitAll)
- auth: `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` on `/api/auth/admin-method` (USER 403 / ADMIN 200)
- demand-account: security + oauth2-resource-server deps; `jwk-set-uri: ${AUTH_JWKS_URI:http://localhost:8083/oauth2/jwks}`
- demand-account: `web/SecurityConfig` (health+docs permitAll, `anyRequest().authenticated()`, `rolesConverter()` maps `roles` claim) + `/api/v1/admin/ping` admin endpoint
- tests: slice via `jwt()` post-processor (+`@EnableWebSecurity` + mock `JwtDecoder`); integration via `@TestConfiguration` test `JwtDecoder` + minted RS256 tokens (no committed keys)
- ADR-0009; `steps/step-17/{requests.http,smoke.sh}`; live cross-service run: no-token 401 → auth-token 201 → USER admin-ping 403

**Gotchas:**
- RSA key is ephemeral (generated at startup) — restarting auth rotates it; old tokens 401 until re-login
- `@WebMvcTest` slice fails "No qualifying bean of type HttpSecurity" without `@EnableWebSecurity` on the config
- `@PreAuthorize` deliberately kept OFF transfer service methods (AOP proxy breaks direct-call unit tests); authz lives at the edge
- Automated suite validates with a TEST key, not the live JWKS — real interop proven only by the live run (§12.8)
- JWKS is fetched lazily on first token, then cached — demand-account boots fine with auth down, but first Bearer request 401s

**Callback hooks:**
- JWKS = issuer's public keys; validators verify but can't forge (Step 18 threat-models it; Step 41 Authorization Server/Keycloak; Step 43 mTLS)
- Revocation gap of self-contained JWTs → short-lived access + refresh tokens (Step 32 frontend flow)
- Roles ride the `roles` claim already `ROLE_`-prefixed (converter prefix = ""), same scheme both services

**Next step starts:** `step-17-end` == `step-18-start`; green: `./mvnw verify` (9 modules, auth 11 + demand-account 31), `smoke.sh` PASSED, §12.3 mutation (permitAll → `expected:<401> but was:<200>`) verified and reverted.
