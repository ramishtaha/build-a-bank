# 🧳 Capsule — Step 16

**Exists now:** 9 Maven modules, `./mvnw verify` green (parent + services/{hello,cif,demand-account,auth}, gateway, playground/{java-basics,spring-lab,concurrency-lab}). New `services/auth` on port 8083 (hello/gateway 8080, cif 8081, demand-account 8082; no Docker/DB): `POST /api/auth/login` (public), `GET /api/auth/me` (authenticated), `GET /api/auth/admin` (ADMIN-only) — 9 tests (AuthSecurityTest 7 + PasswordEncodingTest 2).

**This step added:**
- Stateless `SecurityFilterChain` (lambda DSL): CSRF off, session STATELESS, `/api/auth/login` + `/actuator/health` permitAll, `/api/auth/admin` hasRole(ADMIN), anyRequest authenticated; OAuth2 resource server validating Bearer JWTs.
- HS256 JWTs via Nimbus encoder/decoder over one HMAC secret (`bank.jwt.secret`, ≥32 bytes); `JwtService` issues sub/roles/exp/iss, TTL 30 min (`expiresInSeconds: 1800`).
- In-memory BCrypt user store; fake demo users alice/password (ROLE_USER), admin/admin123 (ROLE_USER + ROLE_ADMIN).
- `steps/step-16/{requests.http,smoke.sh}`, ADR-0008 (dedicated auth service · stateless JWT · HMAC now → asymmetric later · in-memory users), Makefile `run-auth`/`play-16` targets.

**Gotchas:**
- Spring Security 7 grants a `FACTOR_BEARER` authority alongside roles — `/me` filters authorities to `ROLE_*` (observed live, not guessed).
- Claim mapping: `JwtGrantedAuthoritiesConverter` with claim `roles` and an EMPTY prefix (claims already carry `ROLE_`); `hasRole("ADMIN")` needs authority `ROLE_ADMIN`.
- HS256 secret < 32 bytes fails at runtime; `starter-security` without a `SecurityFilterChain` locks everything behind Boot's default login.

**Callback hooks:**
- HMAC = every validator can forge → Step 17 makes the money services/gateway resource servers and moves toward asymmetric/JWKS (Step 41: Authorization Server).
- Demo creds alice/password + admin/admin123, auth port 8083, token TTL 1800s — reused by later requests/tests.
- `cors(Customizer.withDefaults())` is wired but no allow-rules exist yet; real CORS config lands with Step 29's gateway.

**Next step starts:** `step-16-end` == `step-17-start`. Green: `./mvnw verify` (9 modules, BUILD SUCCESS), 9 auth tests, §12.3 mutation (admin permitAll → `expected: 403 but was: 200`, reverted), `smoke.sh` PASSED, clean-room clone verified.
