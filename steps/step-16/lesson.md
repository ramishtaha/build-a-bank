# Step 16 · Spring Security Deep I — The Filter Chain, JWT & Password Encoding
### Phase C — Web, APIs & Application Security 🔵 · Step 16 of 67

> *Until now anyone could call any endpoint. That ends here. You'll build the bank's **Identity & Auth**
> service: a Spring Security **filter chain** that authenticates and authorizes every request, **JWTs** issued
> on login and validated on each call, and **BCrypt** for passwords. By the end you can say exactly what
> happens when a request arrives with — and without — a valid token.*

---

<a id="toc"></a>
## 🧭 The Six Movements of This Step

| | Movement | What happens | ~time |
|---|---|---|---|
| **A** | [🧭 Orient](#orient) | 30-second overview · skip-test · cheat card · why it matters · before you start | ~1h |
| **B** | [🧠 Understand](#understand) | the security filter chain · authn vs authz · JWTs · BCrypt · CSRF/CORS/headers | ~4h |
| **C** | [🛠️ Build](#build) | the `auth` service: filter chain, JWT encode/decode, BCrypt user store, login + protected endpoints | ~9h |
| **D** | [🔬 Prove](#prove) | the Verification Log — login→token→401/403/200 over real HTTP, §12.3 mutation | ~2h |
| **E** | [🎓 Apply](#apply) | go deeper · interview prep · your-turn challenges | ~2.5h |
| **F** | [🏆 Review](#review) | troubleshooting · resources · recap, flashcards & what's next | ~1.5h |

---

<a id="orient"></a>

# A · 🧭 Orient

## 📋 This Step in 30 Seconds

| | |
|---|---|
| **Title** | Spring Security deep I — the security filter chain, JWT authentication, role-based authorization, BCrypt |
| **Step** | 16 of 67 · **Phase C — Web, APIs & Application Security** 🔵 |
| **Effort** | ≈ 20 hours focused. Security is on every backend interview and every real system; this is the foundation the rest of the bank's security builds on. Experienced Spring Security users can skim to ~4h. |
| **What you'll run this step** | **JVM + Maven only** — no Docker, no DB (the user store is in-memory for now). One command: `./mvnw -pl services/auth test`. Run it live with `make run-auth` (port 8083). |
| **Buildable artifact** | A new **`services/auth`** (Identity & Auth) module: a `SecurityFilterChain` (stateless, JWT resource server), `JwtEncoder`/`JwtDecoder` (HMAC HS256), a `BCryptPasswordEncoder` + in-memory user store, and `POST /api/auth/login` (issue a JWT) + `GET /api/auth/me` (authenticated) + `GET /api/auth/admin` (ADMIN-only). **9 tests.** `step-16-start == step-15-end`. |
| **Verification tier** | 🔴 **Full** — a new service + the build *and* a security path. `./mvnw verify` green + all **9** tests + the login→token→401/403/200 flow proven over real HTTP + BCrypt verified + the **§12.3 mutation** (weaken the admin rule → 403 test fails → revert) + clean-room + `smoke.sh`. |
| **Depends on** | **[Step 13](../step-13/lesson.md)** (the request lifecycle / filters — security *is* a filter chain), **[Step 15](../step-15/lesson.md)** (the gateway, the eventual edge for auth). Spring Security is **first used here**. |

By the end you will be able to explain the **security filter chain** and configure it with the lambda DSL; distinguish **authentication** (401) from **authorization** (403); issue and validate **JWTs**; hash passwords with **BCrypt**; and reason about **CSRF/CORS/security headers** for a stateless API.

### ⏭️ Can You Skip This Step? (5-minute self-check)

If you can confidently do **all** of this, skim the 🧩 Pattern Spotlight and jump to **[Step 17 — Spring Security deep II & modern auth](../step-17/lesson.md)**.

- [ ] I can explain the **security filter chain** and configure a `SecurityFilterChain` bean (and know `WebSecurityConfigurerAdapter` was removed in Security 6, `antMatchers`→`requestMatchers`).
- [ ] I can distinguish **authentication (401)** from **authorization (403)** and enforce both (`authenticated()`, `hasRole(...)`).
- [ ] I can explain a **JWT** (header.claims.signature), issue one, and validate it as an **OAuth2 resource server**.
- [ ] I can explain **HMAC vs asymmetric** JWT signing and when to use each.
- [ ] I can hash passwords with **BCrypt** and say why (salt, slow, one-way) — and why never store plaintext.
- [ ] I can explain why **CSRF is disabled** for a stateless token API and what **security headers** Spring adds.

> [!TIP]
> Not 100%? Stay. "How does Spring Security work / what's the filter chain?", "401 vs 403?", "how do you validate a JWT?", and "how do you store passwords?" are guaranteed interview questions — and you'll have built and tested all of it.

## 📇 Cheat Card

> **What this step delivers (one sentence):** the bank's Auth service — a stateless Spring Security filter chain that mints JWTs on login (BCrypt-checked credentials) and validates them on every protected call, enforcing authentication (401) and role-based authorization (403), proven end-to-end over real HTTP.

**Key commands** (Windows uses `.\mvnw.cmd`):

```bash
# Build + test the auth service (9 tests, no Docker):
./mvnw -pl services/auth test

# Run it live (port 8083), then drive steps/step-16/requests.http:
./mvnw -pl services/auth spring-boot:run
curl -s -X POST localhost:8083/api/auth/login -H 'Content-Type: application/json' -d '{"username":"alice","password":"password"}'
#   → {"token":"<JWT>","expiresInSeconds":1800}   then:  curl -H "Authorization: Bearer <JWT>" localhost:8083/api/auth/me

# One-shot proof your build matches the lesson:
bash steps/step-16/smoke.sh
```

**The one headline idea — *every request runs the security filter chain first; no/invalid token → 401, valid-but-wrong-role → 403, valid+authorized → your controller*:**

```mermaid
flowchart LR
    req["request + Authorization: Bearer <jwt>"] --> fc["🔒 Security filter chain"]
    fc -->|"no/invalid token"| e401["401 Unauthorized"]
    fc -->|"valid token, wrong role"| e403["403 Forbidden"]
    fc -->|"valid + authorized"| ctrl["your @RestController"]
    login["POST /login (BCrypt check)"] -->|"issues"| jwt["signed JWT (HS256): sub, roles, exp"]
```

*Alt-text: a request with a Bearer JWT enters the security filter chain, which returns 401 if the token is missing/invalid, 403 if it's valid but lacks the required role, and otherwise passes through to the controller. Separately, POST /login checks the password with BCrypt and issues a signed JWT carrying the subject, roles, and expiry.*

## 🎯 Why This Matters

An unauthenticated banking API is a non-starter — and security is the area where mistakes are catastrophic and interviews are relentless. The **filter chain** is *how* Spring Security works (every "how does auth work in Spring?" answer starts there); **JWTs** are how modern stateless APIs and microservices carry identity; **BCrypt** is table-stakes for not leaking passwords in a breach. After this step you've built the bank's front-of-house security and can explain, precisely, the journey of a request through authentication and authorization — exactly what "secure this endpoint" interviews want.

## ✅ What You'll Be Able to Do

- **Configure the filter chain** — a `SecurityFilterChain` bean with the lambda DSL: public vs authenticated vs role-restricted paths.
- **Authenticate with JWT** — issue tokens on login, validate them as an OAuth2 resource server.
- **Authorize by role** — `hasRole(...)`, and map JWT claims to authorities.
- **Hash passwords** — BCrypt, and explain salt/slowness/one-wayness.
- **Reason about CSRF/CORS/headers** — disable CSRF for a stateless token API (and know when to re-enable), and the headers Spring sets.

## 🧰 Before You Start

**Prerequisites**

- ✅ You finished **Step 15**; the repo is at `step-16-start` (== `step-15-end`) and `./mvnw verify` is green.
- ✅ A JDK 21+ (we pin 25). **No Docker/DB this step** — the user store is in-memory.

**What you already learned that connects here**

- **Step 13**: the request lifecycle and servlet filters — Spring Security *is* a chain of filters in front of the `DispatcherServlet`.
- **Step 15**: the gateway — the eventual home for **edge** authentication (Step 17 pushes auth there).
- **Step 14**: HMAC signing (webhook signatures) — JWT signatures are the same idea applied to tokens.

> **Depends on: Steps 15, 13.** This is the first of two security deep-dives (Step 17 adds OAuth2/OIDC + MFA).

## 🗓️ Session Plan

≈ 20 hours ≠ one sitting. Here's the step cut into **8 sittings of ~1.5–3h**, each ending at a real ✋ checkpoint (a commit or a section boundary). One sitting per day makes this step a good week — that's fine; the plan is the pace.

| Sitting | Covers | ~time | Ends at (✋ save point) |
|---|---|---|---|
| **S1** | A Orient (30-seconds → Session Plan) + B: 🧠 The Big Idea + 🧩 Pattern Spotlight | ~2.5h | end of 🧩 Pattern Spotlight |
| **S2** | B: 🌱 Under the Hood → 🛡️ Security Lens → 🕰️ Then vs. Now → 🧵 Thread-safety | ~2.5h | end of movement B |
| **S3** | C: Sub-steps 0–2 (module → `application.yml`/bootstrap → `SecurityConfig`, the heart) | ~3h | sub-step 2 ✋ + commit |
| **S4** | C: Sub-steps 3–5 (`JwtService` → `UserService` → `AuthDtos`) | ~2.5h | sub-step 5 ✋ + commit |
| **S5** | C: Sub-steps 6–7 (`AuthController` — first live login→token→`/me` run — then `AuthSecurityTest`) | ~2.5h | sub-step 7 ✋ + commit |
| **S6** | C: Sub-steps 8–9 (`PasswordEncodingTest`, run all 9, harness + §12.3 mutation) + 🎮 Play With It + 🏁 DoD | ~2.5h | `step-16-end` tagged, DoD checked |
| **S7** | D: 🔬 the Verification Log — re-run §1, §2 and §5 against your own build | ~1.5h | end of movement D |
| **S8** | E Apply (interview prep + one Your-Turn challenge) + F Review (recap, flashcards) | ~3h | sign-off 🚀 |

**Optional routes:** the ⏭️ skip-test (5 min) can shrink this to a ~4h skim for Spring Security veterans; the 🚀 Go Deeper asides in E add ~25 min total; Your-Turn challenges 1–5 are open-ended extras (1–4h each) beyond the 20h.

✋ **Stopping here?** You have the map — the effort budget, the `services/auth` artifact, and prerequisites checked (`./mvnw -q verify` green). Next: B · 🧠 Understand; first action: read `## 🧠 The Big Idea` and the bank-security-desk analogy.

---

<a id="understand"></a>

# B · 🧠 Understand

## 🧠 The Big Idea

Spring Security is, at heart, **a chain of servlet filters** placed in front of your application. Every HTTP request passes through this **security filter chain** *before* it reaches the `DispatcherServlet`/your controller (recall the filter layer from Step 13). The chain's job is two questions, in order:

1. **Authentication — *who are you?*** Establish identity from the request (here: a JWT in the `Authorization: Bearer` header). No credentials, or invalid ones → **401 Unauthorized**.
2. **Authorization — *are you allowed?*** Given the identity, check the request is permitted (e.g. this path needs the `ADMIN` role). Authenticated but not permitted → **403 Forbidden**.

You configure it by declaring a **`SecurityFilterChain` bean** with a fluent **lambda DSL** (the modern way — the old `WebSecurityConfigurerAdapter` was removed in Spring Security 6). You declare, per path, whether it's `permitAll()` (public), `authenticated()` (any valid identity), or role-restricted (`hasRole("ADMIN")`).

**JWT (JSON Web Token)** is how the identity travels. It's three base64url parts — **header . claims . signature** — where the signature (HMAC-SHA256 here) lets anyone with the key verify the token wasn't forged or tampered with. The claims carry the subject (`sub` = username), `roles`, and an expiry (`exp`). On **login** we check the password and **issue** a signed JWT; on every later request the chain **validates** it (as an OAuth2 *resource server*) and builds the `Authentication` from its claims — no server session needed (**stateless**).

**Passwords** are never stored in plaintext. We hash them with **BCrypt**: a deliberately *slow*, *salted*, *one-way* function. Slow defeats brute force; the per-hash salt means identical passwords get different hashes (no rainbow tables); one-way means a database leak doesn't reveal the passwords. You verify by re-hashing the attempt and comparing — Spring's `PasswordEncoder.matches`.

> **Analogy — the bank's security desk.** The **filter chain** is the guard at the door who checks everyone *before* they reach any office. **Authentication** is "show me ID" — no ID, you're turned away at the door (401). **Authorization** is "your ID is valid, but this is the vault floor and you're not cleared for it" — you're stopped at the elevator (403). The **JWT** is a tamper-evident day-pass: issued at the desk after they verify you (login), stamped with who you are, your clearances, and an expiry, and **sealed** (signed) so a forged pass is spotted instantly. **BCrypt** is how the desk stores your PIN — never the PIN itself, only a one-way scramble they can check against, deliberately slow so a stolen ledger of scrambles is useless.

```mermaid
sequenceDiagram
    participant U as User
    participant FC as Security Filter Chain
    participant A as AuthController
    U->>A: POST /login {user, password}  (public)
    A->>A: BCrypt matches? → issue signed JWT (sub, roles, exp)
    A-->>U: 200 { token }
    U->>FC: GET /me  Authorization: Bearer <jwt>
    FC->>FC: validate signature + expiry → build Authentication (roles)
    alt valid + authorized
        FC->>A: forward
        A-->>U: 200 identity
    else missing/invalid → 401 · wrong role → 403
        FC-->>U: 401 / 403
    end
```

*Alt-text: the user logs in (a public endpoint); the controller verifies the password with BCrypt and issues a signed JWT with subject, roles, and expiry. On a later request with the Bearer token, the filter chain validates the signature and expiry, builds the Authentication with roles, and either forwards to the controller (200) or returns 401 (missing/invalid token) or 403 (wrong role).*

❓ **Knowledge-check:** a request arrives with a *valid* JWT, but the token's roles don't include the one the path requires — which status code, and is that an authentication or an authorization failure? <details><summary>answer</summary>**403 Forbidden** — an **authorization** failure. Identity was established (authentication succeeded), but the access rule denied it. No/invalid token would be 401 (authentication).</details>

## 🧩 Pattern Spotlight — Stateless JWT Authentication (Resource Server)

> **Problem.** A microservices platform can't rely on server-side sessions: sessions are stateful (sticky load balancing or a shared session store), and every service would need access to them. You need identity that travels *with the request* and that any service can verify independently.

> **Why stateless JWT fits.** A signed JWT carries the identity and is self-contained: any service holding the verification key can validate it (signature + expiry) **without** a session lookup or a call back to the auth service. That scales horizontally (no sticky sessions) and decouples services. Spring models the validating side as an **OAuth2 resource server**.

> **How it works (the mechanism).** Login verifies the password and signs a JWT (`JwtEncoder`). Each later request carries `Authorization: Bearer <jwt>`; the resource-server filter decodes and validates it (`JwtDecoder`: signature via the shared HMAC secret + `exp`), maps the `roles` claim to authorities, and sets the `Authentication` — all stateless. `SessionCreationPolicy.STATELESS` tells Spring not to create/use a session.

> **Alternatives / trade-offs.** **Sessions + cookies** are simple for a single server-rendered app and support easy server-side revocation, but are stateful and CSRF-prone. **Opaque tokens** (a random string the server looks up) give instant revocation but require a lookup per request (back to stateful). JWTs are great for scale/decoupling but are **hard to revoke** before expiry (mitigate with short lifetimes + refresh tokens, Step 17/32). **HMAC vs asymmetric** signing: HMAC (one shared secret) is simplest for one issuer+validator; asymmetric (private key signs, public key/JWKS validates) is the move when many services validate (Step 17/41) — they can verify without being able to forge.

> **Implementation (here).** `SecurityConfig` wires `oauth2ResourceServer(jwt(...))` with an HMAC `JwtDecoder`; `JwtService` issues tokens with `JwtEncoder`; `AuthController` logs in and exposes protected endpoints. `AuthSecurityTest` proves the 401/403/200 outcomes.

## 🌱 Under the Hood: How It Really Works

**The filter chain, concretely.** Spring Security registers a `FilterChainProxy` (one servlet filter) that delegates to your `SecurityFilterChain` — itself an ordered list of filters (e.g. `BearerTokenAuthenticationFilter` for resource-server JWT, `AuthorizationFilter` for access rules, exception-translation, etc.). The `BearerTokenAuthenticationFilter` extracts the `Authorization: Bearer` token, hands it to the `JwtDecoder`, and on success sets the `Authentication` in the `SecurityContext`; the `AuthorizationFilter` then checks your `authorizeHttpRequests` rules. On failure: the `AuthenticationEntryPoint` returns **401** (not authenticated), the `AccessDeniedHandler` returns **403** (authenticated, not authorized).

**`SecurityFilterChain` + the lambda DSL.** You expose a `@Bean SecurityFilterChain filterChain(HttpSecurity http)`. The DSL: `authorizeHttpRequests(a -> a.requestMatchers("/api/auth/login").permitAll().requestMatchers("/api/auth/admin").hasRole("ADMIN").anyRequest().authenticated())`, plus `oauth2ResourceServer(...)`, `csrf(...)`, `sessionManagement(...)`. (History: `WebSecurityConfigurerAdapter` was removed in 6.0; `antMatchers`→`requestMatchers`; `authorizeRequests`→`authorizeHttpRequests`.) `hasRole("ADMIN")` requires the authority `ROLE_ADMIN` (the `ROLE_` prefix is added for you).

**Issuing & validating JWTs (Nimbus).** `NimbusJwtEncoder(new ImmutableSecret<>(secretKey))` signs; `NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(HS256).build()` validates. A token is built from a `JwtClaimsSet` (`issuer`, `subject`, `issuedAt`, `expiresAt`, custom `roles`) + a `JwsHeader`. The decoder checks the **signature** (so a tampered/forged token is rejected) and the **expiry** (so old tokens die). HS256 needs a secret ≥ 256 bits (32 bytes) — ours is.

**Mapping claims to authorities.** A `JwtAuthenticationConverter` + `JwtGrantedAuthoritiesConverter` reads the `roles` claim into Spring authorities. We set `authoritiesClaimName("roles")` and an empty prefix (our claim already carries `ROLE_`), so a token with `roles: ["ROLE_USER"]` grants authority `ROLE_USER` → `hasRole("USER")` works.

**BCrypt.** `BCryptPasswordEncoder.encode("password")` → a `$2a$10$...` string embedding the cost factor and a random 16-byte salt; `matches(raw, hash)` re-derives and compares in constant time. The cost factor (work) makes each guess expensive — you tune it up as hardware improves. *Never* `equals` on a password; *never* store plaintext or a fast unsalted hash (MD5/SHA-1).

**Why CSRF is disabled here.** CSRF (cross-site request forgery) tricks a browser into sending a request with the user's **ambient** credentials — i.e. cookies/session sent automatically. A **stateless Bearer-token** API has no cookie/session that rides along automatically (the client must explicitly attach the token), so there's nothing for CSRF to exploit; disabling it is correct (and required, or Spring would block our non-browser clients). If we later add **cookie-based browser sessions**, CSRF protection comes back on for those flows.

**Security headers.** Spring Security sets safe defaults on responses — e.g. `X-Content-Type-Options: nosniff`, `Cache-Control` for protected resources, `X-Frame-Options` (clickjacking). We assert `nosniff` is present.

**A Spring Security 7 detail (verify, don't guess).** SS7 introduced **authentication factors**: a JWT/bearer authentication also grants a `FACTOR_BEARER` authority alongside your roles. It's an internal marker (useful for step-up auth, Step 17), not a "role" — so our `/me` filters authorities to `ROLE_*` to keep the API contract clean. (We discovered this by *running it* — the raw `/me` initially returned `["FACTOR_BEARER","ROLE_USER"]`.)

## 🛡️ Security Lens: What Could Go Wrong

- **Storing passwords wrong is the classic breach.** Plaintext, or a fast unsalted hash (MD5/SHA-1), means a DB leak hands attackers every password (and reused passwords elsewhere). BCrypt (slow + salted) is the minimum; Argon2/scrypt are alternatives. We hash at rest and verify with `matches`.
- **JWT pitfalls.** A weak/leaked HMAC secret lets anyone forge tokens — keep it long and secret (Vault, Phase H); rotate it. Accepting `alg: none` or letting the client choose the algorithm is a classic JWT exploit — we pin HS256 on the decoder. JWTs are **hard to revoke** before expiry — keep lifetimes short + use refresh tokens (Step 17/32). Never put secrets/PII in claims (they're readable — only *signed*, not encrypted).
- **401 vs 403 leakage.** Returning 403 (vs 404) can reveal that a resource exists; returning detailed auth errors can help attackers. Keep responses generic; we return bare 401/403.
- **Disable CSRF *only* because we're stateless.** Disabling CSRF on a cookie/session app is a real vulnerability. The rule is "no ambient credentials → no CSRF risk"; know which mode you're in.
- **Don't trust unverified tokens.** Every protected request must run the decoder (signature + expiry). The filter chain does this for us — never parse a JWT "just to read it" and trust the contents without verifying the signature.

## 🕰️ Then vs. Now (How This Changed Across Versions)

| Topic | Then | Now | Why it changed |
|---|---|---|---|
| **Config style** | `WebSecurityConfigurerAdapter` (subclass + override). | A **`SecurityFilterChain` bean** + lambda DSL. | The adapter was **removed in Spring Security 6**; component-based config is clearer and composable. |
| **Matchers / rules** | `antMatchers(...)`, `authorizeRequests(...)`. | **`requestMatchers(...)`**, **`authorizeHttpRequests(...)`**. | Renamed/clarified in 5.8/6; `antMatchers` removed. |
| **Tokens** | Server sessions + cookies (stateful). | **Stateless JWT** (OAuth2 resource server) for APIs/microservices. | Scales horizontally, decouples services, no shared session store. |
| **Auth factors** | n/a | Spring Security **7** grants `FACTOR_*` authorities (e.g. `FACTOR_BEARER`) to model the authentication method. | Enables step-up / multi-factor reasoning (Step 17). |

> [!NOTE]
> *Verify, don't guess.* `WebSecurityConfigurerAdapter` removed in Security 6; `requestMatchers`/`authorizeHttpRequests` are current. We're on **Spring Security 7** (Boot 4) — verified the `SecurityFilterChain` DSL, Nimbus `JwtEncoder`/`JwtDecoder` (HS256), `BCryptPasswordEncoder`, and the resource-server JWT flow all **resolve and work** (9 tests + a live login→token→401/403/200 run, 🔬). The SS7 `FACTOR_BEARER` authority is a real, observed behaviour (we filter it out of `/me`). All deps are Boot-managed.

## 🧵 Thread-safety note

Spring Security's components are **stateless singletons** safe to share across request threads: the filter chain, `JwtDecoder`/`JwtEncoder`, and `BCryptPasswordEncoder` hold no per-request mutable state. The **per-request** identity lives in the `SecurityContextHolder`, which is backed by a **`ThreadLocal`** (and cleared at the end of each request) — so each request thread sees only its own `Authentication`, never another's. Our in-memory user store uses a `ConcurrentHashMap`. This is Step 11's "stateless singletons + confine per-request state" rule, applied to security.

✋ **Stopping here?** You have the mental model — filter chain, 401 vs 403, JWT anatomy, BCrypt, the CSRF rationale. Next: C · 🛠️ Build, Sub-step 0 (the auth module); first action: create `services/auth/pom.xml`.

---

<a id="build"></a>

# C · 🛠️ Build

## 📦 Your Starting Point

You're at **`step-16-start`** (== `step-15-end`). The services exist (`hello`, `cif`, `demand-account`, `gateway`, plus the `playground/*` and `libs/common` modules) but are **completely unsecured** — anyone can call any endpoint. We add a brand-new `services/auth` module; nothing existing is touched, so no existing test changes (that's the whole point of ADR-0008 — see 🚀 Go Deeper). **No Docker/DB this step** — an in-memory user store; the DB + OIDC + MFA arrive in Step 17+.

Confirm the start builds (green from Step 15):

```bash
./mvnw -q verify
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

If that's green, you're ready. Everything below is **new code** in `services/auth`.

## 🛠️ Let's Build It — Step by Step

We build the auth service bottom-up: the module, then its config, then the security machinery (filter chain, token issuer, user store), then the API, then the tests, then the play/verify harness. Ten small sub-steps — run between each.

```mermaid
flowchart TB
    a["0 · auth module pom + root pom"] --> b["1 · application.yml + AuthApplication"]
    b --> c["2 · SecurityConfig: filter chain + JwtEncoder/Decoder + BCrypt"]
    c --> d["3 · JwtService (issue signed tokens)"]
    d --> e["4 · UserService (in-memory BCrypt store)"]
    e --> f["5 · AuthDtos (request/response records)"]
    f --> g["6 · AuthController: /login, /me, /admin"]
    g --> h["7 · AuthSecurityTest (real-HTTP authn/authz)"]
    h --> i["8 · PasswordEncodingTest (BCrypt) + run all 9"]
    i --> j["9 · requests.http + smoke.sh + §12.3 mutation"]
```

*Alt-text: a ten-box flow — module pom, config + main class, SecurityConfig, JwtService, UserService, DTOs, controller, security test, password test + run, then the play/verify harness.*

🌳 **Files we'll touch:**

```
pom.xml                                                  (+ <module>services/auth</module>)
services/auth/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/buildabank/auth/
    │   │   ├── AuthApplication.java
    │   │   ├── security/
    │   │   │   ├── SecurityConfig.java          # the filter chain (the heart)
    │   │   │   └── JwtService.java              # issue signed JWTs
    │   │   ├── user/
    │   │   │   └── UserService.java             # in-memory BCrypt store
    │   │   └── web/
    │   │       ├── AuthController.java          # /login, /me, /admin
    │   │       └── AuthDtos.java                # request/response records
    │   └── resources/
    │       └── application.yml
    └── test/java/com/buildabank/auth/
        ├── AuthSecurityTest.java                # real-HTTP authn/authz (7 tests)
        └── PasswordEncodingTest.java            # BCrypt unit (2 tests)
steps/step-16/{requests.http, smoke.sh} · adr/0008-auth-service-and-jwt-security.md
```

---

### Sub-step 0 of 9 — The auth module · ≈45 min 🧭 *(you are here: **module** → config → SecurityConfig → JwtService → UserService → DTOs → controller → security test → password test → harness)*

🎯 **Goal:** stand up a new Maven module with the right dependencies — Spring Security (the filter chain + BCrypt) and OAuth2 Resource Server (Bearer-JWT validation **and**, via Nimbus, the library we use to *issue* tokens) — and register it in the parent build.

📁 **Location:** new file → `services/auth/pom.xml`, plus one line added to the root `pom.xml`.

⌨️ **Code** (the full module pom):

```xml
<!-- services/auth/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!--
      auth — the Identity & Auth service. Spring Security deep I (Step 16): the security filter chain,
      authentication vs authorization, JWT issue + validate (HMAC, as an OAuth2 resource server), BCrypt
      password encoding, CSRF/CORS/security headers. The tokens it issues secure the other services from
      Step 17. In-memory user store for now (DB + OIDC + MFA come later).
    -->
    <parent>
        <groupId>com.buildabank</groupId>
        <artifactId>build-a-bank-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>auth</artifactId>
    <name>Build-a-Bank :: Services :: Auth</name>
    <description>Identity &amp; Auth — security filter chain, JWT, BCrypt (Step 16).</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <!-- OAuth2 Resource Server brings spring-security-oauth2-jose (Nimbus) for JWT encode/decode. -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- ── Test ── -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

📁 **Now register the module** → edit the root `pom.xml` `<modules>` block (a one-line addition):

```diff
  <modules>
        <module>services/hello</module>
        <module>services/cif</module>
        <module>services/demand-account</module>
+       <module>services/auth</module>
        <module>gateway</module>
        <module>playground/java-basics</module>
        <module>playground/spring-lab</module>
```

🔍 **Line-by-line:**

- `<parent>` — inherit everything from the root `build-a-bank-parent` POM: the Spring Boot BOM (so dependency versions are **managed** — note none of our `<dependency>` entries carry a `<version>`), the pinned JDK, Spotless, the wrapper. `<relativePath>../../pom.xml</relativePath>` points up two levels (out of `services/auth/`) to that parent.
- `<artifactId>auth</artifactId>` — this module's coordinate; combined with the inherited group/version it's `com.buildabank:auth:0.1.0-SNAPSHOT`.
- `spring-boot-starter-web` — Spring MVC + embedded Tomcat: this is a normal web service that also happens to be secured.
- **`spring-boot-starter-security`** — the star of the step: pulls in the **`FilterChainProxy`**, the lambda-DSL config support, and **`BCryptPasswordEncoder`**. Adding it alone secures *everything* with Boot's default form login — which is why sub-step 2's `SecurityFilterChain` is mandatory to make it behave as we want.
- **`spring-boot-starter-oauth2-resource-server`** — adds the Bearer-token filter (`BearerTokenAuthenticationFilter`) for validating incoming JWTs, **and** transitively brings `spring-security-oauth2-jose`, which wraps the **Nimbus** JOSE library — the same library we use to *issue* (`NimbusJwtEncoder`) and *decode* (`NimbusJwtDecoder`) tokens.
- `spring-boot-starter-validation` — Jakarta Bean Validation, so `@Valid @RequestBody LoginRequest` rejects blank credentials (sub-step 5–6).
- `spring-boot-starter-actuator` — gives us `/actuator/health` (we mark it public in the chain) and the operational endpoints.
- **Test deps:** `spring-boot-starter-test` (JUnit 5 + AssertJ + JsonPath), `spring-boot-webmvc-test` (Boot 4's MVC test slice support), and **`spring-security-test`** — Spring Security's MockMvc-style test helpers (`@WithMockUser`, security request post-processors). Note: it is *not* what makes the real chain run in our integration test — a `RANDOM_PORT` test boots a real server, so the real filter chain always applies (sub-step 7).
- `spring-boot-maven-plugin` — repackages the runnable fat-jar and powers `spring-boot:run`.
- The **root-pom diff** adds `services/auth` to the reactor so `./mvnw verify` builds it with everything else, in dependency order.

💭 **Under the hood:** Maven's reactor reads every `<module>` to build a dependency graph and a build order. Because `auth` depends only on Boot starters (not on a sibling module), it can build independently. The Boot BOM (inherited via the parent) is a giant `<dependencyManagement>` list that pins compatible versions for *all* Spring/Security/Nimbus artifacts — that's why "no versions" here still resolves a coherent, tested set (Boot 4.0.6 → Spring Security 7).

🔮 **Predict:** if you add `starter-security` but *forget* to add a `SecurityFilterChain` bean (sub-step 2), what happens when you hit `GET /api/auth/me`? <details><summary>answer</summary>Spring Boot auto-configures a **default** chain that secures *every* request with HTTP Basic / form login and a generated password printed at startup — so you'd get a 401 (or a login page), not your intended rules. Our explicit chain replaces that default.</details>

▶️ **Run & See** — resolve the new module's dependencies:

```bash
./mvnw -q -pl services/auth dependency:resolve
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** the `auth` module resolves; `./mvnw -q -pl services/auth dependency:resolve` is green and you can see `spring-security-oauth2-jose` (Nimbus) in the resolved tree.

💾 **Commit:**

```bash
git add services/auth/pom.xml pom.xml
git commit -m "build(auth): add Spring Security + OAuth2 resource server module"
```

⚠️ **Pitfall:** just adding `starter-security` with **no** `SecurityFilterChain` secures everything with Boot's default login and a random startup password — so the next sub-step's config is what makes the service behave as designed. (Also: a missing `<relativePath>` makes Maven hit the remote repo for the parent and fail — keep the `../../pom.xml`.)

---

### Sub-step 1 of 9 — `application.yml` + `AuthApplication` · ≈30 min 🧭 *(module ✅ → **config** → SecurityConfig → JwtService → …)*

🎯 **Goal:** configure the service (port, JWT secret/TTL/issuer, graceful shutdown) and add the Spring Boot bootstrap class.

📁 **Location:** new file → `services/auth/src/main/resources/application.yml`

⌨️ **Code:**

```yaml
# services/auth/src/main/resources/application.yml
spring:
  application:
    name: auth

bank:
  jwt:
    # DEMO secret only (fake — never a real secret in git; Vault in Phase H). HS256 needs >= 32 bytes.
    secret: ${BANK_JWT_SECRET:dev-only-change-me-build-a-bank-hmac-secret-key-256bit-minimum}
    ttl-minutes: 30
    issuer: build-a-bank-auth

server:
  port: 8083                 # auth's port (hello/gateway 8080, cif 8081, demand-account 8082)
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info

logging:
  level:
    org.springframework.security: INFO
    com.buildabank.auth: INFO
```

📁 **Now the bootstrap class** → `services/auth/src/main/java/com/buildabank/auth/AuthApplication.java`

```java
// services/auth/src/main/java/com/buildabank/auth/AuthApplication.java
package com.buildabank.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The Identity & Auth service: issues and validates JWTs, secured by Spring Security (Step 16). */
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

🔍 **Line-by-line:**

- `spring.application.name: auth` — the service's logical name (shows in logs, later in tracing/metrics tags).
- **`bank.jwt.secret: ${BANK_JWT_SECRET:dev-only-change-me-...}`** — externalized config with a **default**: read the env var `BANK_JWT_SECRET` if set, else fall back to the long demo string. The fallback is a **fake** dev secret (never a real one in git; Vault in Phase H). The colon-default syntax `${VAR:fallback}` is Spring's property placeholder. The string is deliberately ≥ 32 characters because **HS256 needs a ≥ 256-bit (32-byte) key** or it throws at startup.
- `ttl-minutes: 30` — access-token lifetime; `JwtService.ttlSeconds()` reports `30 × 60 = 1800` seconds to clients.
- `issuer: build-a-bank-auth` — the `iss` claim stamped into every token (who minted it).
- `server.port: 8083` — auth's port (each bank service has its own: gateway/hello 8080, cif 8081, demand-account 8082, **auth 8083**). `shutdown: graceful` lets in-flight requests finish on stop.
- `management...include: health,info` — expose only `/actuator/health` and `/info` over HTTP (we mark `/actuator/health` public in the chain).
- `logging.level.org.springframework.security: INFO` — keep security logs readable; bump to `DEBUG` when debugging the filter chain.
- **`@SpringBootApplication`** — the meta-annotation that turns on component scanning (finds our `@Configuration`/`@Service`/`@RestController`), auto-configuration (wires Security, Web, Actuator from the classpath), and configuration-properties binding. `SpringApplication.run(...)` boots the embedded Tomcat.

💭 **Under the hood:** at startup Spring binds `bank.jwt.*` into the `SecurityConfig`/`JwtService` constructors via `@Value`. Because `starter-security` is on the classpath, Boot's auto-config would install a default chain — but our explicit `@Bean SecurityFilterChain` (next sub-step) **wins** and replaces it. `@SpringBootApplication` sits in the **root package** `com.buildabank.auth`, so component scanning covers `security`, `user`, and `web` sub-packages automatically.

🔮 **Predict:** what port will the live service listen on, and what status will `GET /actuator/health` return (no token)? <details><summary>answer</summary>Port **8083**; `/actuator/health` is `permitAll()` in the chain, so it returns **200** `{"status":"UP"}` without a token.</details>

▶️ **Run & See** — compile the module so far:

```bash
./mvnw -q -pl services/auth -DskipTests compile
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `application.yml` and `AuthApplication` compile. (The app won't *start* meaningfully yet without a controller, but it compiles.)

💾 **Commit:**

```bash
git add services/auth/src/main/resources/application.yml services/auth/src/main/java/com/buildabank/auth/AuthApplication.java
git commit -m "feat(auth): app config (port 8083, JWT secret/TTL/issuer) + bootstrap class"
```

⚠️ **Pitfall:** committing a **real** secret here. The default is fake and clearly labelled; in production the value comes from `BANK_JWT_SECRET` (env/Vault), never the YAML. A secret shorter than 32 bytes will fail HS256 at runtime — keep the demo string long.

✋ **Stopping here?** You have the auth module skeleton (module POM registered, `application.yml`, `AuthApplication`) compiling, committed. Next: Sub-step 2 (`SecurityConfig` — the heart of the step); first action: create `services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java`.

---

### Sub-step 2 of 9 — `SecurityConfig`: the filter chain + JWT + BCrypt · ≈1.5h 🧭 *(module ✅ → config ✅ → **SecurityConfig** → JwtService → …)*

🎯 **Goal:** the **heart of the step** — declare *who can hit what* (the access rules), make the API **stateless** (no session, CSRF off), wire JWT **validation** (resource server) + the password and token-signing beans. This one file is what every "how does Spring Security work?" interview answer is really about.

📁 **Location:** new file → `services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java`

⌨️ **Code** (the complete file):

```java
// services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java
package com.buildabank.auth.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * The heart of Step 16: the <strong>security filter chain</strong> + the JWT and password machinery.
 *
 * <p>This is a <strong>stateless</strong> API secured by JWTs (no server session, no cookies), so we disable
 * CSRF (there's no cookie/session for a CSRF attack to ride) and set the session policy to STATELESS. The
 * authorization rules decide, per request path, what's public vs. requires authentication vs. requires a role.
 * As an OAuth2 <em>resource server</em>, Spring validates the {@code Authorization: Bearer <jwt>} on every
 * protected request using the {@link JwtDecoder}; we issue those same tokens with the {@link JwtEncoder}.
 */
@Configuration
public class SecurityConfig {

    private final byte[] secret;
    private final MacAlgorithm macAlgorithm = MacAlgorithm.HS256;

    public SecurityConfig(@Value("${bank.jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);   // HS256 needs >= 32 bytes (256 bits)
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT API: no session, no cookies → CSRF is not applicable (and would block our clients).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/actuator/health").permitAll()   // public
                        .requestMatchers("/api/auth/admin").hasRole("ADMIN")                   // authZ: role required
                        .anyRequest().authenticated())                                         // everything else: authN
                // Validate incoming Bearer JWTs; map the "roles" claim to Spring authorities.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    /** BCrypt for password hashing — slow-by-design + per-hash salt (never store or compare plaintext). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Signs JWTs with the shared HMAC secret. */
    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    /** Validates JWTs (signature + expiry) with the same HMAC secret. */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(macAlgorithm).build();
    }

    MacAlgorithm macAlgorithm() {
        return macAlgorithm;
    }

    SecretKeySpec secretKey() {
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    /** Maps the token's {@code roles} claim (already like "ROLE_USER") straight to granted authorities. */
    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");   // the claim already carries the ROLE_ prefix
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
```

🔍 **Line-by-line:**

- `@Configuration` — a class that contributes `@Bean` definitions to the context. Spring will call its `@Bean` methods at startup and manage the results as singletons.
- **constructor `@Value("${bank.jwt.secret}")`** — Spring injects the resolved secret string (from `application.yml`/env) and we convert it to bytes once. `StandardCharsets.UTF_8` makes the byte encoding explicit (don't rely on the platform default). The comment flags the **≥ 32-byte** HS256 requirement.
- `private final MacAlgorithm macAlgorithm = MacAlgorithm.HS256` — pin the algorithm in **one** place; both the encoder header and decoder use it. (Pinning the algorithm on the decoder is also a security control — see the pitfall.)
- **`@Bean SecurityFilterChain filterChain(HttpSecurity http)`** — the modern way to configure Spring Security: a bean built from the `HttpSecurity` builder. This replaces the removed `WebSecurityConfigurerAdapter`. Each `.xxx(...)` configures one aspect; `http.build()` produces the chain.
  - `.csrf(csrf -> csrf.disable())` — turn **CSRF protection off**: correct *because* this is a stateless Bearer-token API with no cookie/session for a forgery to ride (and leaving it on would block our non-browser clients).
  - `.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))` — Spring will **not** create or use an `HttpSession`; identity is re-established from the token on every request.
  - `.cors(Customizer.withDefaults())` — enable CORS with sensible defaults (so a browser SPA from another origin can call us later; Step 29+).
  - **`.authorizeHttpRequests(...)`** — the access rules, evaluated **top-down, first match wins**: `/api/auth/login` and `/actuator/health` are `permitAll()` (public); `/api/auth/admin` needs `hasRole("ADMIN")`; `anyRequest().authenticated()` catches everything else (needs *some* valid identity). **Order matters** — put the specific rules before `anyRequest()`.
  - **`.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))`** — register the Bearer-token filter that validates `Authorization: Bearer <jwt>` using our `JwtDecoder` bean, then converts claims → authorities via our converter.
- **`@Bean PasswordEncoder passwordEncoder()`** → `BCryptPasswordEncoder` — the password-hashing strategy (slow, salted, one-way). Injected into `UserService`.
- **`@Bean JwtEncoder jwtEncoder()`** → `NimbusJwtEncoder(new ImmutableSecret<>(secretKey()))` — signs tokens with the shared HMAC secret. `ImmutableSecret` is Nimbus's wrapper that exposes the secret as a JWK source.
- **`@Bean JwtDecoder jwtDecoder()`** → `NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(HS256).build()` — validates tokens: checks the **signature** with the same secret and pins the algorithm to **HS256**. The resource server uses this bean automatically.
- `secretKey()` → `new SecretKeySpec(secret, "HmacSHA256")` — wraps the raw bytes as a JCA secret key for HMAC-SHA256 (shared by encoder + decoder).
- **`jwtAuthConverter()`** — `JwtGrantedAuthoritiesConverter` with `authoritiesClaimName("roles")` (read authorities from the `roles` claim, not the default `scope`/`scp`) and `authorityPrefix("")` (the claim already contains `ROLE_USER`, so **don't** add another `ROLE_` prefix). Wrapped in a `JwtAuthenticationConverter`. Result: a token with `roles: ["ROLE_USER"]` grants authority `ROLE_USER`, which `hasRole("USER")` checks.

💭 **Under the hood:** `http.build()` assembles an ordered list of servlet filters (the `SecurityFilterChain`), fronted by a single `FilterChainProxy`. For each request: the `BearerTokenAuthenticationFilter` pulls the token, calls `JwtDecoder.decode` (signature + `exp` checks), and on success populates the `SecurityContext` with an `Authentication` whose authorities came from our converter. Then the `AuthorizationFilter` evaluates the `authorizeHttpRequests` rules. Missing/invalid token → the `AuthenticationEntryPoint` writes **401**; valid token but rule denies → the `AccessDeniedHandler` writes **403**. Your controller runs only if both pass.

🔮 **Predict:** a request to `/api/auth/me` with **no** `Authorization` header — what status? And `/api/auth/login` with no token? <details><summary>answer</summary>`/me` → **401** (it falls to `anyRequest().authenticated()`, no identity). `/login` → reaches the controller (it's `permitAll()`). Both proven in 🔬.</details>

▶️ **Run & See** — compile (the security beans wire up):

```bash
./mvnw -q -pl services/auth -DskipTests compile
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `SecurityConfig` compiles; you can name the three access rules and the three security beans (`PasswordEncoder`, `JwtEncoder`, `JwtDecoder`).

💾 **Commit:**

```bash
git add services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java
git commit -m "feat(auth): stateless JWT SecurityFilterChain + BCrypt + HS256 encoder/decoder"
```

⚠️ **Pitfall:** **letting the client pick the algorithm.** Always pin `.macAlgorithm(HS256)` on the decoder; a decoder that accepts `alg: none` or any algorithm is the classic JWT-bypass exploit. Also: rule **order** — if `anyRequest().authenticated()` came *before* `/login`'s `permitAll()`, login itself would need a token (chicken-and-egg). Specific rules first.

---

### Sub-step 3 of 9 — `JwtService`: mint signed tokens · ≈1h 🧭 *(… SecurityConfig ✅ → **JwtService** → UserService → …)*

🎯 **Goal:** a service that builds a JWT's claims (`sub`, `roles`, `iss`, `iat`, `exp`) and **signs** it with the `JwtEncoder` from sub-step 2 — returning the compact `header.claims.signature` string.

📁 **Location:** new file → `services/auth/src/main/java/com/buildabank/auth/security/JwtService.java`

⌨️ **Code** (the complete file):

```java
// services/auth/src/main/java/com/buildabank/auth/security/JwtService.java
package com.buildabank.auth.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues signed JWTs. A JWT is three base64url parts — header, claims, signature — where the signature
 * (HMAC-SHA256 here) lets any holder of the secret verify the token wasn't tampered with. We put the
 * username in {@code sub}, the roles in a {@code roles} claim, and an expiry in {@code exp} so the token is
 * short-lived.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long ttlMinutes;
    private final String issuer;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${bank.jwt.ttl-minutes:30}") long ttlMinutes,
                      @Value("${bank.jwt.issuer:build-a-bank-auth}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.ttlMinutes = ttlMinutes;
        this.issuer = issuer;
    }

    /** Mint a signed JWT for the user with their roles, valid for {@code ttlMinutes}. */
    public String issue(String username, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(ttlMinutes)))
                .subject(username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return ttlMinutes * 60;
    }
}
```

🔍 **Line-by-line:**

- `@Service` — a `@Component` stereotype: Spring creates one singleton and injects it into `AuthController`.
- **constructor injection** — `JwtEncoder` is the bean from `SecurityConfig`; `ttlMinutes` and `issuer` come from `bank.jwt.*` via `@Value` with inline defaults (`:30`, `:build-a-bank-auth`) so the service is usable even if those keys are absent. All three fields are `final` (immutable → thread-safe singleton).
- **`issue(String username, List<String> roles)`** — the one job: build + sign a token.
  - `Instant now = Instant.now()` — UTC instant (the bank's time discipline: always `Instant`, never `LocalDateTime`).
  - `JwtClaimsSet.builder()...build()` — the **payload**: `issuer(iss)`, `issuedAt(iat)`, `expiresAt(exp = now + ttl)`, `subject(sub = username)`, and a custom `.claim("roles", roles)` carrying e.g. `["ROLE_USER"]`.
  - `JwsHeader.with(MacAlgorithm.HS256).build()` — the **header**, declaring the signing algorithm (`alg: HS256`).
  - `jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue()` — Nimbus serializes header + claims to base64url, computes the **HMAC-SHA256 signature** over them with the secret, and returns the compact `xxxxx.yyyyy.zzzzz` string.
- **`ttlSeconds()`** — convenience for the API response (`expiresInSeconds`), so a client knows when to re-login/refresh. `30 × 60 = 1800`.

💭 **Under the hood:** the token's first two parts (header + claims) are just **base64url — readable by anyone** (paste it into jwt.io). The third part, the **signature**, is what proves authenticity: only someone holding the secret can produce a signature that the `JwtDecoder` will accept. So a JWT is **signed, not encrypted** — never put secrets/PII in claims. Changing a single claim byte invalidates the signature → the decoder rejects it (sub-step 7's tamper experiments rely on exactly this).

🔮 **Predict:** if you decode the middle (claims) part of a token issued for `alice`, what JSON do you expect to see? <details><summary>answer</summary>Something like `{"iss":"build-a-bank-auth","sub":"alice","exp":...,"iat":...,"roles":["ROLE_USER"]}` — exactly what the Verification Log §2 shows when it base64-decodes a real token.</details>

▶️ **Run & See** — compile:

```bash
./mvnw -q -pl services/auth -DskipTests compile
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `JwtService` compiles; you can name the five claims it sets (`iss`, `iat`, `exp`, `sub`, `roles`).

💾 **Commit:**

```bash
git add services/auth/src/main/java/com/buildabank/auth/security/JwtService.java
git commit -m "feat(auth): JWT issuer (HS256, sub/roles/exp claims, configurable TTL)"
```

⚠️ **Pitfall:** putting secrets or PII in claims because "it's encoded." Base64 is **not** encryption — anyone can read the payload. The signature only proves *integrity/authenticity*, not *confidentiality*. Keep claims to identity + roles + timing.

✋ **Stopping here?** You have the filter chain + BCrypt/JWT beans and the token issuer compiling, committed. Next: Sub-step 4 (`UserService` — the BCrypt store); first action: create `services/auth/src/main/java/com/buildabank/auth/user/UserService.java`.

---

### Sub-step 4 of 9 — `UserService`: in-memory BCrypt store · ≈45 min 🧭 *(… JwtService ✅ → **UserService** → DTOs → …)*

🎯 **Goal:** a tiny user store that holds **only BCrypt hashes** (never plaintext) and verifies credentials with a constant-time `matches`. Seeded with two fake demo users.

📁 **Location:** new file → `services/auth/src/main/java/com/buildabank/auth/user/UserService.java`

⌨️ **Code** (the complete file):

```java
// services/auth/src/main/java/com/buildabank/auth/user/UserService.java
package com.buildabank.auth.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * A tiny in-memory user store with <strong>BCrypt-hashed</strong> passwords (a real DB-backed store, plus
 * OIDC, comes in Step 17+). Passwords are never stored or compared in plaintext: we keep only the BCrypt
 * hash and verify with a constant-time {@link PasswordEncoder#matches}.
 */
@Service
public class UserService {

    /** A stored user: username, the BCrypt hash (never the plaintext), and granted roles. */
    public record StoredUser(String username, String passwordHash, List<String> roles) {
    }

    private final PasswordEncoder passwordEncoder;
    private final Map<String, StoredUser> users = new ConcurrentHashMap<>();

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        // Seed demo users (fake credentials only). Passwords are hashed at startup — never persisted in clear.
        register("alice", "password", List.of("ROLE_USER"));
        register("admin", "admin123", List.of("ROLE_USER", "ROLE_ADMIN"));
    }

    private void register(String username, String rawPassword, List<String> roles) {
        users.put(username, new StoredUser(username, passwordEncoder.encode(rawPassword), roles));
    }

    /** Verify credentials with BCrypt; returns the user only if the password matches. */
    public Optional<StoredUser> authenticate(String username, String rawPassword) {
        StoredUser user = users.get(username);
        if (user != null && passwordEncoder.matches(rawPassword, user.passwordHash())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
```

🔍 **Line-by-line:**

- `@Service` — singleton; Spring injects the `PasswordEncoder` (the BCrypt bean from `SecurityConfig`).
- **`record StoredUser(String username, String passwordHash, List<String> roles)`** — an immutable value carrying the username, the **BCrypt hash** (never the raw password), and the roles. A nested `record` is perfect for a small immutable carrier (contrast Step 8's *entity*, which can't be a record).
- `private final Map<String, StoredUser> users = new ConcurrentHashMap<>()` — the store, keyed by username. **`ConcurrentHashMap`** because many request threads read it concurrently (Step 11's safe-publication rule).
- **constructor** — seeds two **fake** demo users: `alice/password` (ROLE_USER) and `admin/admin123` (ROLE_USER + ROLE_ADMIN). They're hashed **at startup** via `register`, so even in memory we never hold the plaintext.
- `register(...)` — `passwordEncoder.encode(rawPassword)` produces the `$2a$10$...` BCrypt hash; only that goes into the map.
- **`authenticate(username, rawPassword)`** — look up the user; if present, `passwordEncoder.matches(rawPassword, user.passwordHash())` re-hashes the attempt with the stored salt/cost and compares in **constant time**. Returns `Optional<StoredUser>` — present iff the password matched. Note we never compare strings with `equals`.

💭 **Under the hood:** `BCryptPasswordEncoder.encode` generates a fresh random 16-byte salt, runs the bcrypt key-schedule `2^cost` times (cost 10 by default → 1024 rounds), and emits `$2a$10$<22-char-salt><31-char-hash>`. `matches` parses the cost+salt back out of the stored string, re-derives, and compares — which is why the **same password encoded twice yields different strings yet both verify** (sub-step 8 proves this). The deliberate slowness (~tens of ms) is the whole point: it makes offline brute-force of a leaked hash table impractical.

🔮 **Predict:** `authenticate("alice", "password")` vs `authenticate("alice", "PASSWORD")` — what does each return? <details><summary>answer</summary>The first returns `Optional.of(alice)` (match); the second returns `Optional.empty()` (BCrypt is case-sensitive — wrong password). The controller turns empty into a 401.</details>

▶️ **Run & See** — compile:

```bash
./mvnw -q -pl services/auth -DskipTests compile
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `UserService` compiles; the store holds hashes, not plaintext, and `authenticate` returns an `Optional`.

💾 **Commit:**

```bash
git add services/auth/src/main/java/com/buildabank/auth/user/UserService.java
git commit -m "feat(auth): in-memory BCrypt user store (alice/admin demo users)"
```

⚠️ **Pitfall:** seeding with plaintext, or logging the raw password (it leaks to log aggregation). Always hash at the boundary; never log credentials. And use a `ConcurrentHashMap` (or proper synchronization) — a plain `HashMap` mutated/read across request threads is a data race (Step 11).

---

### Sub-step 5 of 9 — `AuthDtos`: request/response records · ≈30 min 🧭 *(… UserService ✅ → **DTOs** → controller → …)*

🎯 **Goal:** the small immutable records that shape the API — the login request (validated) and the two responses (token, identity). DTOs keep the wire contract separate from internals.

📁 **Location:** new file → `services/auth/src/main/java/com/buildabank/auth/web/AuthDtos.java`

⌨️ **Code** (the complete file):

```java
// services/auth/src/main/java/com/buildabank/auth/web/AuthDtos.java
package com.buildabank.auth.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/** Request/response records for the auth API (grouped to keep the package tidy). */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Login credentials. */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** Issued token + how long it's valid (seconds). */
    public record TokenResponse(String token, long expiresInSeconds) {
    }

    /** The authenticated principal's identity, derived from the validated JWT. */
    public record MeResponse(String username, List<String> roles) {
    }
}
```

🔍 **Line-by-line:**

- `public final class AuthDtos` with a `private AuthDtos() {}` — a **holder class** that only groups records; `final` + private constructor means it's never subclassed or instantiated. Pure namespacing.
- **`record LoginRequest(@NotBlank String username, @NotBlank String password)`** — the login body. `@NotBlank` (Jakarta Bean Validation) rejects null/empty/whitespace-only values; combined with `@Valid` on the controller param, a blank username/password yields a **400** before any logic runs.
- **`record TokenResponse(String token, long expiresInSeconds)`** — the login success body: the compact JWT + its lifetime in seconds (1800). Serialized by Jackson to `{"token":"...","expiresInSeconds":1800}`.
- **`record MeResponse(String username, List<String> roles)`** — the `/me` body: the subject + the (ROLE-only) authorities. → `{"username":"alice","roles":["ROLE_USER"]}`.
- All three are **records** → immutable, with accessors Jackson uses to (de)serialize JSON. Perfect for DTOs (unlike entities).

💭 **Under the hood:** Jackson maps record components to JSON fields by name (`username` → `"username"`). On the way in, it constructs `LoginRequest` via its canonical constructor; Bean Validation then runs because of `@Valid`. On the way out, it reads the accessor methods (`token()`, `expiresInSeconds()`) to build the JSON. Records give you the immutable, boilerplate-free DTO Spring + Jackson want.

🔮 **Predict:** `POST /api/auth/login` with body `{"username":"","password":"x"}` — what status, given `@NotBlank` + `@Valid`? <details><summary>answer</summary>**400 Bad Request** — validation fails on the blank username before authentication runs. (Wrong-but-present credentials, by contrast, give 401 — different failure, different code.)</details>

▶️ **Run & See** — compile:

```bash
./mvnw -q -pl services/auth -DskipTests compile
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `AuthDtos` compiles; three records exist (`LoginRequest`, `TokenResponse`, `MeResponse`).

💾 **Commit:**

```bash
git add services/auth/src/main/java/com/buildabank/auth/web/AuthDtos.java
git commit -m "feat(auth): request/response DTO records (LoginRequest, TokenResponse, MeResponse)"
```

⚠️ **Pitfall:** reusing the entity/`StoredUser` as the API type would leak the **password hash** to clients. Always map to a DTO that exposes only what the API should — here `MeResponse` carries username + roles, never the hash.

---

### Sub-step 6 of 9 — `AuthController`: login + protected endpoints · ≈1.5h 🧭 *(… DTOs ✅ → **controller** → security test → …)*

🎯 **Goal:** the API surface — `POST /api/auth/login` (public; BCrypt-check → issue token or 401), `GET /api/auth/me` (any valid token), `GET /api/auth/admin` (ADMIN role). Crucially, the controller has **no authorization code** — the filter chain enforced it before we run.

📁 **Location:** new file → `services/auth/src/main/java/com/buildabank/auth/web/AuthController.java`

⌨️ **Code** (the complete file):

```java
// services/auth/src/main/java/com/buildabank/auth/web/AuthController.java
package com.buildabank.auth.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.auth.security.JwtService;
import com.buildabank.auth.user.UserService;
import com.buildabank.auth.web.AuthDtos.LoginRequest;
import com.buildabank.auth.web.AuthDtos.MeResponse;
import com.buildabank.auth.web.AuthDtos.TokenResponse;

/**
 * The auth API. {@code /login} is public (it issues tokens); {@code /me} requires a valid token
 * (authentication); {@code /admin} additionally requires the ADMIN role (authorization) — the security
 * filter chain enforces the last two before this controller is ever reached.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService users;
    private final JwtService jwt;

    public AuthController(UserService users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    /** Authenticate (BCrypt) and issue a JWT → 200 with the token, or 401 if the credentials are wrong. */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return users.authenticate(request.username(), request.password())
                .map(user -> ResponseEntity.ok(
                        new TokenResponse(jwt.issue(user.username(), user.roles()), jwt.ttlSeconds())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Who am I? Reads the identity from the validated JWT (the filter chain populated the Authentication). */
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        // Report only roles — Spring Security 7 also grants authentication-factor authorities (e.g.
        // FACTOR_BEARER) which are an internal detail, not part of this API's role contract.
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .sorted().toList();
        return new MeResponse(authentication.getName(), roles);
    }

    /** ADMIN-only — reachable only with a token carrying ROLE_ADMIN (else the filter chain returns 403). */
    @GetMapping("/admin")
    public Map<String, String> admin() {
        return Map.of("message", "admin access granted");
    }
}
```

🔍 **Line-by-line:**

- `@RestController` — controller whose return values are written straight to the HTTP body as JSON (Jackson), no view resolution.
- `@RequestMapping("/api/auth")` — base path; all three endpoints hang off it.
- **constructor injection** of `UserService` + `JwtService` (final fields).
- **`@PostMapping("/login")`** with **`@Valid @RequestBody LoginRequest request`** — bind+validate the JSON body. The flow: `users.authenticate(...)` → on match, `.map(...)` builds a `TokenResponse` (`jwt.issue(...)` + `jwt.ttlSeconds()`) wrapped in `200 OK`; on empty, `.orElseGet(...)` returns **401 Unauthorized** with no body. (`/login` is `permitAll()`, so the chain lets it through unauthenticated.)
- **`@GetMapping("/me")` with an `Authentication` parameter** — Spring injects the `Authentication` the **filter chain built** from the validated JWT. `getName()` is the `sub` (username). We stream the authorities, keep only `ROLE_*` (filtering out SS7's `FACTOR_BEARER`), sort, and return them in a `MeResponse`. There's **no token-parsing code here** — the chain already validated it.
- **`@GetMapping("/admin")`** — returns a static message. Note: **no auth check in the method** — the chain's `requestMatchers("/api/auth/admin").hasRole("ADMIN")` already returned 403 for non-admins before this runs. Authorization lives in **one** place.

💭 **Under the hood:** for `/me` and `/admin`, by the time your method runs, the `BearerTokenAuthenticationFilter` has already decoded+validated the JWT and the `AuthorizationFilter` has already applied the rule. So the controller is blissfully simple — it *reads* identity, it doesn't *establish* it. The `Authentication` comes from the `SecurityContextHolder` (a `ThreadLocal`), which is why method-arg injection works and is request-confined.

❓ **Knowledge-check:** the `/admin` handler contains zero security code — what stops a `ROLE_USER` token from reaching it? <details><summary>answer</summary>The **security filter chain**: sub-step 2's `requestMatchers("/api/auth/admin").hasRole("ADMIN")` rule is enforced by the `AuthorizationFilter` *before* the controller is invoked, returning 403. Authorization lives in one place (the chain), not in controller bodies.</details>

🔮 **Predict:** before you run it — `GET /api/auth/admin` with **alice's** token (ROLE_USER) returns what? With **admin's** token? <details><summary>answer</summary>alice → **403** (valid token, wrong role); admin → **200** `{"message":"admin access granted"}`. The Verification Log §2 shows both.</details>

▶️ **Run & See** — start the service and drive the live flow:

```bash
./mvnw -pl services/auth spring-boot:run     # starts on port 8083
# in a second terminal:
TOKEN=$(curl -s -X POST localhost:8083/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s -H "Authorization: Bearer $TOKEN" localhost:8083/api/auth/me
```

✅ **Expected output** (real run — see Verification Log §2):

```
{"username":"alice","roles":["ROLE_USER"]}
```

The startup log confirms the stack (genuine, from a fresh run today):

```
 :: Spring Boot ::                (v4.0.6)
... Starting AuthApplication using Java 25.0.3 ...
... Tomcat started on port 8083 (http) with context path '/'
... Started AuthApplication in ~2.8 seconds
```

✋ **Checkpoint:** login returns a `{token, expiresInSeconds}` JSON; `/me` with the token returns your identity; `/me` with no token → 401; `/admin` as alice → 403.

💾 **Commit:**

```bash
git add services/auth/src/main/java/com/buildabank/auth/web/AuthController.java
git commit -m "feat(auth): login + /me + /admin endpoints"
```

⚠️ **Pitfall:** duplicating authorization in the controller body (e.g. an `if (!isAdmin) throw ...`) when the chain already enforces `hasRole("ADMIN")`. Keep authorization in **one** place (the chain, or method security — Step 17) so the two can't drift out of sync.

✋ **Stopping here?** You have a live login→token→`/me` flow working over real HTTP, committed. Next: Sub-step 7 (`AuthSecurityTest`); first action: create `services/auth/src/test/java/com/buildabank/auth/AuthSecurityTest.java`.

---

### Sub-step 7 of 9 — `AuthSecurityTest`: authn/authz over real HTTP · ≈1h 🧭 *(… controller ✅ → **security test** → password test → …)*

🎯 **Goal:** prove the whole thing end-to-end over **real HTTP** (random port, real filter chain): valid login → token; wrong password → 401; no token → 401 (authN); wrong role → 403 (authZ); right role → 200; and the `nosniff` security header is set.

📁 **Location:** new file → `services/auth/src/test/java/com/buildabank/auth/AuthSecurityTest.java`

⌨️ **Code** (the complete file — this is the step-16-end version; later steps add two more tests, §12.8):

```java
// services/auth/src/test/java/com/buildabank/auth/AuthSecurityTest.java
package com.buildabank.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end security over real HTTP: log in to get a JWT, then use it. Proves the filter chain enforces
 * <strong>authentication</strong> (no token → 401) and <strong>authorization</strong> (wrong role → 403),
 * that valid credentials mint a usable token, and that Spring Security's default security headers are set.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSecurityTest {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private String base;

    @BeforeEach
    void setup() {
        base = "http://localhost:" + port;
    }

    @Test
    void login_withValidCredentials_returnsAToken() throws Exception {
        HttpResponse<String> response = login("alice", "password");
        assertThat(response.statusCode()).isEqualTo(200);
        String token = JsonPath.read(response.body(), "$.token");
        assertThat(token).isNotBlank().contains(".");   // a JWT has dot-separated parts
    }

    @Test
    void login_withWrongPassword_isRejected() throws Exception {
        assertThat(login("alice", "WRONG").statusCode()).isEqualTo(401);
    }

    @Test
    void me_withoutToken_is401() throws Exception {
        assertThat(get("/api/auth/me", null).statusCode()).isEqualTo(401);   // authentication required
    }

    @Test
    void me_withValidToken_returnsIdentity() throws Exception {
        String token = tokenFor("alice", "password");
        HttpResponse<String> me = get("/api/auth/me", token);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(me.body(), "$.username")).isEqualTo("alice");
        assertThat(me.body()).contains("ROLE_USER");
    }

    @Test
    void admin_asNonAdmin_is403() throws Exception {
        String userToken = tokenFor("alice", "password");          // ROLE_USER only
        assertThat(get("/api/auth/admin", userToken).statusCode()).isEqualTo(403);   // authorization denied
    }

    @Test
    void admin_asAdmin_is200() throws Exception {
        String adminToken = tokenFor("admin", "admin123");         // ROLE_ADMIN
        assertThat(get("/api/auth/admin", adminToken).statusCode()).isEqualTo(200);
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        HttpResponse<String> response = login("alice", "password");
        // Spring Security sets safe defaults on every response.
        assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    }

    // ── helpers ──
    private String tokenFor(String username, String password) throws Exception {
        return JsonPath.read(login(username, password).body(), "$.token");
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return post("/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
```

🔍 **Line-by-line:**

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` — boots the **full** application context on a **random** free port (so parallel test runs don't clash) with a **real embedded Tomcat**. Because a real server handles the requests, the **real filter chain** runs — unauthenticated requests genuinely get 401. (`spring-security-test` isn't what does this — it's only needed for MockMvc-style tests, `@WithMockUser` and friends, not this class.) `@LocalServerPort int port` is injected with the chosen port.
- **`HttpClient http = HttpClient.newHttpClient()`** — the JDK's HTTP client (Java 11+). We hit the running server over real TCP — this is an integration test, not a mock.
- `@BeforeEach setup()` — build the base URL from the random port.
- `login_withValidCredentials_returnsAToken` — POST valid creds → **200**; `JsonPath.read(body, "$.token")` extracts the token; assert it's non-blank and dot-separated (a JWT shape).
- `login_withWrongPassword_isRejected` → **401** (BCrypt mismatch → empty `Optional` → 401).
- `me_withoutToken_is401` → **401** — proves **authentication** is enforced (no identity).
- `me_withValidToken_returnsIdentity` → **200** with `username:"alice"` and `ROLE_USER` — the happy path.
- **`admin_asNonAdmin_is403`** → **403** with alice's ROLE_USER token — proves **authorization**. *(This is the test the §12.3 mutation breaks.)*
- `admin_asAdmin_is200` → **200** with admin's token (ROLE_ADMIN).
- `securityHeadersArePresent` → asserts `X-Content-Type-Options: nosniff` is on the response (Spring's safe defaults).
- **helpers** — `tokenFor` (login → extract token), `login` (POST creds), `post`/`get` (thin `HttpClient` wrappers; `get` adds the `Authorization: Bearer` header only when a token is supplied).

💭 **Under the hood:** `RANDOM_PORT` + the JDK `HttpClient` exercise the *real* network path: DNS-free localhost TCP, Tomcat, the `FilterChainProxy`, the `BearerTokenAuthenticationFilter` decoding the very token a prior `login` call minted. The 401 (no token) comes from the `AuthenticationEntryPoint`; the 403 (wrong role) from the `AccessDeniedHandler` — different filters, different codes, both asserted. This is the highest-confidence test: if it's green, the chain genuinely behaves as designed.

🔮 **Predict:** these 7 tests boot the full Spring context once. Will the bound port be `8083` or something else? <details><summary>answer</summary>**Something else** — `RANDOM_PORT` picks a free port (a fresh run today used **58369**); `application.yml`'s `8083` is only for `spring-boot:run`. A *fixed* port in an integration test is a smell (clashes, masks port bugs).</details>

▶️ **Run & See** — (we run the full suite in the next sub-step). For now, compile the test sources:

```bash
./mvnw -q -pl services/auth -DskipTests test-compile
```

✅ **Expected (tail):**

```
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `AuthSecurityTest` compiles; you can point to the assertions for 401 (authN) and 403 (authZ).

💾 **Commit:**

```bash
git add services/auth/src/test/java/com/buildabank/auth/AuthSecurityTest.java
git commit -m "test(auth): real-HTTP authn/authz (401/403/200), JWT flow, security header"
```

⚠️ **Pitfall:** mocking the filter chain (or `@WithMockUser` for an integration test) hides the very thing you want to prove. `@SpringBootTest(RANDOM_PORT)` + a real HTTP client runs the *actual* chain — so a 401 is a *real* 401.

---

### Sub-step 8 of 9 — `PasswordEncodingTest` (BCrypt) + run all 9 · ≈45 min 🧭 *(… security test ✅ → **password test** → harness)*

🎯 **Goal:** a fast pure-unit test pinning BCrypt's three properties (one-way, salted, verify-by-`matches`), then run the **whole suite** — 9 tests green.

📁 **Location:** new file → `services/auth/src/test/java/com/buildabank/auth/PasswordEncodingTest.java`

⌨️ **Code** (the complete file):

```java
// services/auth/src/test/java/com/buildabank/auth/PasswordEncodingTest.java
package com.buildabank.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt fundamentals (pure unit): the stored value is a one-way hash (never the plaintext), the same
 * password hashes differently each time (a random per-hash salt), and verification is by {@code matches},
 * not equality.
 */
class PasswordEncodingTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void hashIsNotThePlaintext_andVerifies() {
        String hash = encoder.encode("password");

        assertThat(hash).isNotEqualTo("password");      // never store plaintext
        assertThat(hash).startsWith("$2");              // BCrypt prefix
        assertThat(encoder.matches("password", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }

    @Test
    void samePasswordHashesDifferently_dueToSalt() {
        String a = encoder.encode("password");
        String b = encoder.encode("password");

        assertThat(a).isNotEqualTo(b);                  // distinct salts → distinct hashes...
        assertThat(encoder.matches("password", a)).isTrue();   // ...yet both verify
        assertThat(encoder.matches("password", b)).isTrue();
    }
}
```

🔍 **Line-by-line:**

- No Spring context — a plain JUnit test with its own `BCryptPasswordEncoder` (fast: milliseconds, no Tomcat).
- **`hashIsNotThePlaintext_andVerifies`** — `encode("password")` ≠ `"password"` (one-way); starts with `$2` (the BCrypt version prefix); `matches("password", hash)` is **true**; `matches("wrong", hash)` is **false**. This is the contract `UserService.authenticate` relies on.
- **`samePasswordHashesDifferently_dueToSalt`** — encoding `"password"` twice gives **different** strings (`a != b`) because each `encode` draws a fresh random salt — yet `matches` verifies **both**. This is *why* BCrypt defeats rainbow tables: identical passwords don't share a hash.

💭 **Under the hood:** the salt is embedded *inside* the `$2a$10$<salt><hash>` string, so `matches` doesn't need it stored separately — it parses the cost+salt out of the stored value, re-derives, and constant-time-compares. The two assertions encode exactly the two facts interviewers probe: "why salt?" (no shared hashes / rainbow tables) and "how do you verify?" (`matches`, never `equals`).

🔮 **Predict:** how many tests total across both classes, and how long will the suite take (the integration class boots Spring once)? <details><summary>answer</summary>**9 total** — `AuthSecurityTest` 7 + `PasswordEncodingTest` 2. Most of the time is the one Spring context boot (~3s); BCrypt unit tests are sub-second.</details>

▶️ **Run & See** — run the full auth suite:

```bash
./mvnw -pl services/auth test
```

✅ **Expected output** (real run, step-16-end):

```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0   (AuthSecurityTest 7 + PasswordEncodingTest 2)
[INFO] BUILD SUCCESS
```

> 🔎 **§12.8 note (verify, don't guess).** **9** is the `step-16-end` count. If you `git checkout main` and re-run, you'll see **11** (`AuthSecurityTest` grows to 9 with `jwksEndpointPublishesPublicKeyOnly` + `methodSecurity_adminMethod_enforcesRole` — the asymmetric-JWKS and method-security work added in **Step 17**). A fresh run today confirmed the auth module still compiles and runs **green** on **Boot 4.0.6 / Java 25.0.3 / Tomcat 11.0.21** (random test port 58369) — the count differs only because the class grew later, not because anything at this step changed.

✋ **Checkpoint:** **9** green tests (or 11 if you're on `main` — see the note).

💾 **Commit:**

```bash
git add services/auth/src/test/java/com/buildabank/auth/PasswordEncodingTest.java
git commit -m "test(auth): BCrypt fundamentals (one-way, salted, verify-by-matches)"
```

⚠️ **Pitfall:** asserting an exact hash string (it changes every run because of the salt). Assert *properties* — prefix `$2`, `matches` true/false, `a != b` — never the literal hash.

✋ **Stopping here?** You have 9 green tests, committed. Next: Sub-step 9 (`requests.http` + `smoke.sh` + the §12.3 mutation); first action: create `steps/step-16/requests.http`.

---

### Sub-step 9 of 9 — Play & verify harness: `requests.http`, `smoke.sh`, the §12.3 mutation · ≈45 min 🧭 *(… password test ✅ → **harness**)*

🎯 **Goal:** ship the learner aids — a ready-to-run `requests.http`, a one-shot `smoke.sh`, and the **§12.3 mutation** that proves the authorization rule is load-bearing.

📁 **Location:** `steps/step-16/requests.http`, `steps/step-16/smoke.sh`

⌨️ **`steps/step-16/requests.http`** (drive every endpoint from VS Code / IntelliJ HTTP Client, or copy the `curl` equivalents):

```http
### Build-a-Bank · Step 16 · Spring Security deep I — JWT auth, filter chain, BCrypt
### Start the auth service:  ./mvnw -pl services/auth spring-boot:run     (port 8083, no DB needed)
### Demo users (fake creds): alice/password (ROLE_USER) · admin/admin123 (ROLE_USER, ROLE_ADMIN)

@host = http://localhost:8083

### 1) Log in → 200 with a JWT (copy the token into @token below). Wrong password → 401.
# @name login
POST {{host}}/api/auth/login
Content-Type: application/json

{"username":"alice","password":"password"}

### (paste the token from the login response)
@token = PASTE_JWT_HERE

### 2) Who am I? — needs a valid token (no token → 401)
GET {{host}}/api/auth/me
Authorization: Bearer {{token}}

### 3) /me WITHOUT a token → 401 Unauthorized (authentication required)
GET {{host}}/api/auth/me

### 4) /admin as alice (ROLE_USER) → 403 Forbidden (authorization denied)
GET {{host}}/api/auth/admin
Authorization: Bearer {{token}}

### 5) Log in as admin, then /admin → 200 (admin@admin123 has ROLE_ADMIN)
POST {{host}}/api/auth/login
Content-Type: application/json

{"username":"admin","password":"admin123"}

### (paste the admin token, then call /admin)
GET {{host}}/api/auth/admin
Authorization: Bearer PASTE_ADMIN_JWT_HERE
```

📁 **`steps/step-16/smoke.sh`** (the one-shot proof — no Docker):

```bash
#!/usr/bin/env bash
# steps/step-16/smoke.sh — proves the Step-16 Spring Security work (no Docker needed):
# the security filter chain (401 without a token, 403 for the wrong role, 200 with the right one),
# JWT issue + validate (login mints a token, /me reads the identity from it), and BCrypt password hashing.
# Run from the repo root:  bash steps/step-16/smoke.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
MVNW="./mvnw"; [ -x "$MVNW" ] || MVNW="mvn"

echo "==> Build + test the auth service (filter chain, JWT, BCrypt, authn/authz)"
$MVNW -B -q -pl services/auth test

echo "✅ Step 16 smoke test PASSED"
```

🔍 **Line-by-line (smoke.sh):** `set -euo pipefail` — fail on any error, unset var, or broken pipe. `ROOT=...; cd "$ROOT"` — run from the repo root regardless of where it's invoked. `MVNW="./mvnw"; [ -x ... ]` — prefer the wrapper, fall back to a system `mvn`. The single `test` run subsumes compile + all 9 tests; the final echo only prints if the suite passed (because of `set -e`). **No seed file is needed** — the two demo users (`alice`, `admin`) are seeded in-memory at startup by `UserService`.

💭 **Under the hood:** `requests.http`'s `# @name login` lets the HTTP Client capture the login response so you can reference its token in later requests (or just paste it into `@token`). `curl` equivalents are in the Cheat Card and Play With It. The HTTP Client and `curl` both attach `Authorization: Bearer <jwt>` — exactly what the `BearerTokenAuthenticationFilter` reads.

🔮 **Predict:** before running the mutation below — if you change `/api/auth/admin` from `hasRole("ADMIN")` to `permitAll()`, which single test fails, and with what message? <details><summary>answer</summary>`admin_asNonAdmin_is403` fails with `expected: 403 but was: 200` — a non-admin now reaches the admin endpoint. Proven in 🔬 §3.</details>

▶️ **Run & See** — the smoke test, then the **§12.3 mutation** (break it on purpose, watch the test catch it, revert):

```bash
bash steps/step-16/smoke.sh
```

✅ **Expected output:**

```
==> Build + test the auth service (filter chain, JWT, BCrypt, authn/authz)
✅ Step 16 smoke test PASSED
```

🔬 **Break-it (the §12.3 mutation):** in `SecurityConfig`, change `.requestMatchers("/api/auth/admin").hasRole("ADMIN")` to `.requestMatchers("/api/auth/admin").permitAll()`, then `./mvnw -pl services/auth test`:

```
[ERROR] AuthSecurityTest.admin_asNonAdmin_is403
expected: 403
 but was: 200
[INFO] BUILD FAILURE
```

A non-admin now reaches the admin endpoint — proving the test genuinely verifies the role rule. **Revert** the change → green again. (This is the load-bearing-test discipline from §12.3.)

✋ **Checkpoint:** `smoke.sh` prints `✅ Step 16 smoke test PASSED`; the mutation makes `admin_asNonAdmin_is403` fail; reverting restores green.

💾 **Commit:**

```bash
git add steps/step-16/requests.http steps/step-16/smoke.sh adr/0008-auth-service-and-jwt-security.md
git commit -m "docs(step-16): requests.http + smoke.sh + ADR-0008 (auth service, JWT, BCrypt)"
git tag step-16-end
```

⚠️ **Pitfall:** forgetting to **revert** the mutation before tagging. Always restore the code (and re-run to confirm green) — the mutation is a *proof*, not a change you keep.

---

### 🔁 The full flow you just built

```mermaid
sequenceDiagram
    participant C as curl
    participant FC as Filter chain
    participant A as AuthController
    C->>A: POST /api/auth/login {alice, password}
    A->>A: BCrypt matches → JWT(sub=alice, roles=[ROLE_USER], exp)
    A-->>C: 200 {token, expiresInSeconds:1800}
    C->>FC: GET /api/auth/admin  Bearer <alice token>
    FC->>FC: valid token, but role USER ≠ ADMIN
    FC-->>C: 403 Forbidden
    C->>A: GET /api/auth/me  Bearer <alice token>
    A-->>C: 200 {"username":"alice","roles":["ROLE_USER"]}
```

*Alt-text: curl logs in as alice; the controller verifies the password with BCrypt and returns a JWT (sub=alice, roles=[ROLE_USER], exp) with a 1800-second expiry. A call to /admin with alice's token is rejected 403 by the filter chain (USER is not ADMIN); a call to /me with the token returns alice's identity (200).*

## 🎮 Play With It

1. **Run it:** `make run-auth` (port 8083, no DB). Open `steps/step-16/requests.http`.
2. **Log in & call:** `POST /api/auth/login` (alice/password) → copy the token → `GET /api/auth/me` with `Authorization: Bearer <token>`.
3. **See authn/authz:** `/me` with no token → **401**; `/admin` as alice → **403**; log in as `admin`/`admin123` → `/admin` → **200**.
4. **Decode the JWT:** paste the token into https://jwt.io — read the header (`alg: HS256`) and claims (`sub`, `roles`, `exp`, `iat`). You can *read* it without the secret; you can't *forge* it.
5. 🧪 **Little experiments:** tamper one character of the token → `/me` → 401 (signature fails); wait past the expiry → 401; change a role claim by hand → 401 (signature no longer matches); send a blank username to `/login` → **400** (Bean Validation, not 401).

## 🏁 The Finished Result

You're at **`step-16-end`** (== `step-17-start`). The bank has an Identity & Auth service issuing and validating JWTs, with role-based access — **9** green tests.

### ✅ Definition of Done (your self-check)
- [ ] `./mvnw -pl services/auth test` is green with **Tests run: 9** (at `step-16-end`).
- [ ] Login mints a JWT; `/me` needs a valid token (401 without); `/admin` needs ADMIN (403 otherwise).
- [ ] Passwords are BCrypt-hashed; CSRF is off (stateless) with the rationale clear.
- [ ] `bash steps/step-16/smoke.sh` prints `✅ Step 16 smoke test PASSED`.
- [ ] You've committed and tagged `step-16-end`.

✋ **Stopping here?** You have all Step-16 artifacts committed and `step-16-end` tagged. Next: D · 🔬 the Verification Log; first action: `./mvnw -pl services/auth test` and compare your output against §1.

---

<a id="prove"></a>

# D · 🔬 Prove It Works — the Verification Log

> **Tier: 🔴 Full** (new service + build + security path). Real pasted output below — a live JWT flow, the §12.3 mutation, and a clean-room verify. No Docker needed this step.

### 1 · `./mvnw -pl services/auth test` — 9 tests green
```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0   (AuthSecurityTest 7 + PasswordEncodingTest 2)
[INFO] BUILD SUCCESS
```
Verified Spring Security 7 + OAuth2 resource server + Nimbus JWT (HS256) + BCrypt **resolve and work on Boot 4.0.6**.

### 2 · Live JWT flow (real HTTP, port 8083)
```
POST /api/auth/login {alice/password} →
{"token":"eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJidWlsZC1hLWJhbmstYXV0aCIsInN1YiI6ImFsaWNlIiwiZXhwIjoxNzgxMDQxMTkw
LCJpYXQiOjE3ODEwMzkzOTAsInJvbGVzIjpbIlJPTEVfVVNFUiJdfQ.-R6mjJM4RyDYcSU3-OVp3li3wUCjac1iE5CA1H22i7I","expiresInSeconds":1800}
```
The payload (middle part) base64-decodes to: `{"iss":"build-a-bank-auth","sub":"alice","exp":1781041190,"iat":1781039390,"roles":["ROLE_USER"]}`.
```
POST /login {alice/WRONG}                 → 401     (bad password)
GET  /api/auth/me     (no token)          → 401     (authentication required)
GET  /api/auth/me     (alice token)       → 200  {"username":"alice","roles":["ROLE_USER"]}
GET  /api/auth/admin  (alice, ROLE_USER)  → 403     (authorization denied)
POST /login {admin/admin123}; GET /admin  → 200  {"message":"admin access granted"}
```

### 3 · §12.3 Mutation sanity-check — the authorization rule is load-bearing
Changed `/api/auth/admin` from `hasRole("ADMIN")` to `permitAll()`, reran `admin_asNonAdmin_is403`:
```
[ERROR] AuthSecurityTest.admin_asNonAdmin_is403
expected: 403
 but was: 200
[INFO] BUILD FAILURE
```
A non-admin now reaches the admin endpoint — proving the test verifies the role rule. Reverted; green again.

### 4 · BCrypt (PasswordEncodingTest)
The stored value starts `$2` (BCrypt), is **not** the plaintext, `matches("password", hash)` is true and `matches("wrong", hash)` is false; the same password hashes to **different** values (per-hash salt) yet both verify. The X-Content-Type-Options: nosniff header is present on responses.

### 5 · `smoke.sh`
```
==> Build + test the auth service (filter chain, JWT, BCrypt, authn/authz)
✅ Step 16 smoke test PASSED
```

### 6 · Clean-room (§12.4) & chain
Fresh `git clone` at `step-16-end` → `make doctor` + `./mvnw verify` → **BUILD SUCCESS** (all 9 modules). Confirmed `step-16-end` == `step-17-start`.

### 7 · Re-run today (§12.8 honesty about drift)
A fresh `./mvnw -pl services/auth test` against the current tree compiles and runs **green** on **Boot 4.0.6 / Java 25.0.3 / Tomcat 11.0.21** (random test port 58369; `AuthApplication` started in ~2.8 s). The *count* now reads **11** because Step 17 grew `AuthSecurityTest` (added `jwksEndpointPublishesPublicKeyOnly` + `methodSecurity_adminMethod_enforcesRole`). At `step-16-end` the class has **7** tests → **9** total, as recorded in §1. The tag — not `HEAD` — is the source of truth for this step.

---

<a id="apply"></a>

# E · 🎓 Apply

## 🚀 Go Deeper (Optional)

<details>
<summary>① Why a dedicated auth service (and not securing demand-account in place)? — ADR-0008 · +~5 min</summary>

We stood up a **new `services/auth` module** instead of bolting security onto an existing service. The reason (ADR-0008): securing demand-account in place would force authentication into **all of its existing tests at once** — a huge, noisy change that buries the security concepts. A focused auth service teaches the filter chain, JWT, BCrypt, and authn/authz cleanly. **Step 17** then makes demand-account/cif/the gateway *resource servers* that validate this service's tokens — the natural next move, with the concepts already learned in isolation.
</details>

<details>
<summary>② Why asymmetric signing (and a JWKS endpoint) is next · +~10 min</summary>

With HMAC, every service that *validates* a token also holds the secret — so any of them could *forge* tokens. When multiple independent services validate (Step 17+), switch to **asymmetric** signing: the auth service signs with a **private** key; each service validates with the **public** key, fetched from a `/.well-known/jwks.json` (**JWKS**) endpoint. Validators can verify but not mint — least privilege. Spring's resource server supports `jwkSetUri` out of the box. (Full **Authorization Server** in Step 41.)
</details>

<details>
<summary>③ Method security (@PreAuthorize) vs URL rules · +~5 min</summary>

We authorized by URL (`authorizeHttpRequests`). Spring also offers **method security** (`@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` / `@PostAuthorize` / `@PreFilter`) — authorization expressed on the service/method, closer to the domain and reusable across entry points. Use URL rules for coarse edges and method security for fine-grained domain rules. Step 17 goes deeper.
</details>

<details>
<summary>④ Token revocation & refresh · +~5 min</summary>

A JWT is valid until it expires — you can't easily revoke it mid-life (no server lookup). Mitigations: **short** access-token lifetimes (minutes) + a **refresh token** (longer-lived, revocable, used to mint new access tokens), or a denylist of revoked token ids (reintroduces some state). The access/refresh split is standard OAuth2 (Step 17/32 for the frontend flow).
</details>

❓ **Knowledge-check:** with our HS256 setup, every service that can *validate* a token could also *forge* one — why, and what's the fix when many services validate? <details><summary>answer</summary>HMAC is **symmetric**: the same shared secret both signs and verifies, so any holder can mint valid tokens. The fix is **asymmetric** signing — the auth service signs with a private key; validators fetch the public key (JWKS) and can verify but not forge (Step 17/41).</details>

## 💼 Interview Prep: Questions You'll Be Asked

1. **"How does Spring Security work — what's the filter chain?"** *(the classic)* → A chain of servlet filters in front of the app; each request is authenticated (identity established, else 401) then authorized (access rules checked, else 403) before reaching the controller. Configured via a `SecurityFilterChain` bean + lambda DSL (`WebSecurityConfigurerAdapter` removed in 6).

2. **"401 vs 403?"** *(gotcha)* → 401 Unauthorized = not authenticated (no/invalid credentials); 403 Forbidden = authenticated but not allowed (wrong role/permission). Different stages of the chain (`AuthenticationEntryPoint` vs `AccessDeniedHandler`).

3. **"What's a JWT and how do you validate it?"** → A signed token (header.claims.signature). Validate the signature (with the secret/public key) and the expiry; map claims to authorities. As a resource server, Spring's `BearerTokenAuthenticationFilter` + `JwtDecoder` do this per request — stateless, no session.

4. **"Symmetric vs asymmetric JWT signing?"** *(version/architecture)* → HMAC (one shared secret): simple, but every validator can also forge — fine for one issuer+validator. Asymmetric (RSA/EC): issuer signs with a private key, services validate with the public key (JWKS) without being able to mint — use when many services validate.

5. **"How do you store passwords?"** → A slow, salted, one-way hash — **BCrypt** (or Argon2/scrypt), never plaintext or fast unsalted hashes (MD5/SHA-1). Verify with `PasswordEncoder.matches` (constant-time); tune the cost factor up over time.

6. **"Do you need CSRF protection for a JWT API?"** → No, for a **stateless Bearer-token** API — CSRF rides ambient cookie/session credentials, which a token API doesn't have. Yes, for **cookie/session** flows. Know which mode you're in (disabling CSRF on a cookie app is a real vuln).

> **Behavioral/STAR seed:** *"Tell me about adding security to a system."* → Stood up a stateless JWT auth service (S/T): a `SecurityFilterChain` enforcing authn/authz, BCrypt passwords, signed short-lived tokens (A); proved 401/403/200 with tests and flagged the move to asymmetric signing + refresh tokens as the next steps (R).

## 🏋️ Your Turn: Practice & Challenges

1. **Add method security.** Enable `@EnableMethodSecurity` and protect a method with `@PreAuthorize("hasRole('ADMIN')")`; prove it 403s for a user token. <details><summary>hint</summary>It complements URL rules; both must allow.</details>
2. **Add a refresh endpoint.** `POST /api/auth/refresh` that takes a (longer-lived) refresh token and mints a new short access token.
3. **Switch to asymmetric signing.** Generate an RSA key pair, sign with the private key, validate with the public key, and expose a JWKS endpoint. *(Reference: `solutions/step-16/`.)*
4. **Stretch — DB-backed users.** Replace the in-memory store with a JPA `users` table (BCrypt hashes), Flyway migration, Testcontainers — wiring Step 8's stack into auth.
5. **Stretch — tamper test.** Write a test that flips a byte of a valid token and asserts the call 401s (signature verification).

✋ **Stopping here?** You have the interview answers rehearsed and (at least) one Your-Turn challenge attempted. Next: F · 🏆 Review; first action: skim the 🩺 troubleshooting table, then run through the 🃏 flashcards.

---

<a id="review"></a>

# F · 🏆 Review

## 🩺 Stuck? Troubleshooting & Fixes

| Symptom | Cause | Fix |
|---|---|---|
| Everything is 401, even `/login` | no `SecurityFilterChain`, or `/login` not `permitAll` | define the chain; `requestMatchers("/api/auth/login").permitAll()`. |
| A generated password prints at startup & Basic-auth pops up | `starter-security` on classpath but no custom chain → Boot's default | add the `@Bean SecurityFilterChain` (sub-step 2). |
| HS256 fails at startup | HMAC secret < 32 bytes | use a ≥ 256-bit (≥ 32-char) secret. |
| `/admin` returns 403 for the admin too | role/claim mismatch | token `roles` must contain `ROLE_ADMIN`; converter prefix empty (claim already has `ROLE_`). |
| `/me` shows `FACTOR_BEARER` in roles | Spring Security 7 auth-factor authority | filter authorities to `ROLE_*` (we do). |
| Browser/Postman CSRF errors on POST | CSRF enabled on a token API | disable CSRF for the stateless API (re-enable for cookie sessions). |
| Token "works" but tampering isn't caught | reading the JWT without verifying | always validate via the `JwtDecoder` (the resource server does). |
| Re-run shows **11** tests, not 9 | you're on `main`; Step 17 grew `AuthSecurityTest` | for this step's truth, `git checkout step-16-end` (7+2=9). §12.8. |
| Reset to known-good | — | `git checkout step-16-end && ./mvnw -pl services/auth test`. |

Run `make doctor` if the toolchain itself seems off (JDK/Maven versions).

## 📚 Learn More: Resources & Glossary

- Spring Security reference — **Architecture** (filter chain), **OAuth2 Resource Server (JWT)**, **Password Storage**.
- **RFC 7519** (JWT), **RFC 9457** (errors, Step 13). jwt.io to decode tokens.
- OWASP — Authentication & Password Storage Cheat Sheets.

**Glossary:** **security filter chain / `SecurityFilterChain`** · **authentication (401) vs authorization (403)** · **`authorizeHttpRequests` / `requestMatchers` / `hasRole`** · **JWT (header.claims.signature)** · **resource server / `BearerTokenAuthenticationFilter`** · **`JwtEncoder`/`JwtDecoder` (Nimbus)** · **HMAC (HS256) vs asymmetric (JWKS)** · **claims (`sub`, `roles`, `exp`)** · **BCrypt / salt / cost factor** · **CSRF** · **`SecurityContextHolder` (ThreadLocal)** · **`FACTOR_BEARER`** (SS7). (Full definitions in `docs/glossary.md`.)

## 🏆 Recap & Study Notes

**(a) Key points**
- Spring Security is a **filter chain**: authenticate (else 401) → authorize (else 403) → controller. Configure via a `SecurityFilterChain` bean + lambda DSL.
- **JWT** carries identity (header.claims.signature); validate signature + expiry as a **resource server** — stateless, no session.
- **HMAC** (one secret) is simplest now; **asymmetric** (JWKS) when many services validate (Step 17/41).
- **BCrypt** for passwords (salt, slow, one-way); never plaintext; verify with `matches`.
- **CSRF off** for a stateless token API (no ambient credentials); Spring sets safe security **headers**.

**(b) Key terms:** filter chain, SecurityFilterChain, authn(401)/authz(403), authorizeHttpRequests/requestMatchers/hasRole, JWT, resource server, JwtEncoder/JwtDecoder, HMAC vs asymmetric/JWKS, claims (sub/roles/exp), BCrypt/salt/cost, CSRF, SecurityContextHolder/ThreadLocal.

**(c) 🧠 Test Yourself**
1. Where does authentication happen relative to your controller? <details><summary>answer</summary>Before it — in the security filter chain (a servlet filter in front of the DispatcherServlet).</details>
2. 401 vs 403? <details><summary>answer</summary>401 = not authenticated; 403 = authenticated but not authorized.</details>
3. What does a JWT signature prove, and what does it NOT? <details><summary>answer</summary>Proves authenticity/integrity (not forged/tampered, if you verify); does NOT hide the claims (payload is readable — signed, not encrypted).</details>
4. Why disable CSRF here? <details><summary>answer</summary>Stateless Bearer-token API has no ambient cookie/session for CSRF to ride.</details>
5. Why BCrypt over SHA-256? <details><summary>answer</summary>BCrypt is deliberately slow + salted (defeats brute force/rainbow tables); a fast unsalted hash is crackable at scale.</details>

**(d) 🔗 How this connects**
- **Back to Step 13** (filters/request lifecycle — security is a filter chain), **Step 14** (HMAC — same idea as JWT signatures), **Step 15** (the gateway — future edge auth).
- **Forward:** Step 17 (OAuth2/OIDC, MFA/passkeys, method security, asymmetric signing, identity propagation), Step 32 (frontend token refresh + route guards), Step 41 (Authorization Server), Step 18 (threat modeling / OWASP).

**(e) 🏆 Résumé line / interview talking point earned**
> *"Built a stateless JWT auth service with Spring Security — a `SecurityFilterChain` enforcing authentication and role-based authorization, HS256 JWT issue/validate (OAuth2 resource server), and BCrypt password hashing — proven with a real login→token→401/403/200 flow."*

**(f) ✅ You can now…**
- [ ] Configure the security filter chain (public/authenticated/role-restricted).
- [ ] Issue and validate JWTs; explain HMAC vs asymmetric.
- [ ] Hash passwords with BCrypt and reason about CSRF/headers.
- [ ] Distinguish and enforce authentication (401) vs authorization (403).

**(g) 🃏 Flashcards** *(appended to `docs/flashcards.md`)*
- Q: What is the Spring Security filter chain? · A: ordered servlet filters that authn (else 401) then authz (else 403) each request before the controller.
- Q: 401 vs 403? · A: not authenticated vs authenticated-but-not-allowed.
- Q: How validate a JWT? · A: verify signature + expiry (resource server / JwtDecoder); map claims to authorities.
- Q: HMAC vs asymmetric JWT? · A: shared secret (validators can forge) vs private-sign/public-validate (JWKS, can't forge).
- Q: Why BCrypt? · A: slow + salted + one-way; never store plaintext; verify with matches.
> 🔁 **Revisit in ~1 step** (Step 17: OAuth2/OIDC, MFA, asymmetric signing, method security).

**(h) ✍️ One-line reflection:** *Could you, right now, explain to a teammate exactly what happens when a request arrives with no token vs a valid-but-underprivileged token?*

**(i) Sign-off** 🔒 The bank's door now has a lock and a guard. You built the filter chain, JWTs, and password hashing — the foundation of everything secure that follows. Next: **Step 17 — Spring Security deep II**, with OAuth2/OIDC, MFA & passkeys, and securing the money services. Onward! 🚀
