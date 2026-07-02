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
| **Verification tier** | 🔴 **Full** — a new service + the build *and* a security path. `./mvnw verify` green + all **9** tests + the login→token→401/403/200 flow proven over real HTTP + BCrypt verified + the **§12.3 mutation** (the Step-12 ritual: deliberately weaken the admin rule → 403 test fails → revert) + clean-room (§12.4: a fresh clone builds green) + `smoke.sh`. |
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

≈ 20 hours ≠ one sitting. Here's the step cut into **8 sittings of ~2–3h**, each ending at a real save point (a commit or a section boundary). Do one per day and this step takes a week — that's fine; the plan is the pace.

| Sitting | Covers | ~time | Ends at (save point) |
|---|---|---|---|
| **S1** | A Orient (30-seconds → Before You Start) + B: 🧠 The Big Idea + 🧩 Pattern Spotlight | ~2h | end of Pattern Spotlight |
| **S2** | B: 🌱 Under the Hood → 🛡️ Security Lens → 🕰️ Then vs. Now → 🧵 Thread-safety | ~2h | end of movement B |
| **S3** | C: Sub-step 0 (auth module) + Sub-step 1 (`SecurityConfig`) | ~3h | sub-step 1 commit |
| **S4** | C: Sub-step 2 (`UserService` + `JwtService`) | ~2h | sub-step 2 commit |
| **S5** | C: Sub-step 3 (`AuthController` — first live login→token→`/me` run) | ~2h | sub-step 3 commit |
| **S6** | C: Sub-step 4 (tests + §12.3 mutation) + Sub-step 5 (requests.http, smoke.sh, ADR) | ~2.5h | sub-step 5 commit |
| **S7** | C: 🎮 Play With It + DoD, then D: the Verification Log, tag `step-16-end` | ~2.5h | `step-16-end` tagged |
| **S8** | E Apply (interview prep + one Your-Turn challenge) + F Review (recap, flashcards) | ~3h | sign-off 🚀 |

**Optional routes:** the ⏭️ skip-test (5 min) can shrink this to a ~4h skim for Spring Security veterans; the 🚀 Go Deeper asides in E add ~25 min total; Your-Turn challenges 1–5 are open-ended extras (1–4h each) beyond the 20h.

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

> **Alternatives / trade-offs.** **Sessions + cookies** are simple for a single server-rendered app and support easy server-side revocation, but are stateful and CSRF-prone. **Opaque tokens** (a random string the server looks up) give instant revocation but require a lookup per request (back to stateful). JWTs are great for scale/decoupling but are **hard to revoke before expiry** (mitigate with short lifetimes + refresh tokens, Step 17/32). **HMAC vs asymmetric** signing: HMAC (one shared secret) is simplest for one issuer+validator; asymmetric (private key signs, public key/JWKS validates) is the move when many services validate (Step 17/41) — they can verify without being able to forge.

> **Implementation (here).** `SecurityConfig` wires `oauth2ResourceServer(jwt(...))` with an HMAC `JwtDecoder`; `JwtService` issues tokens with `JwtEncoder`; `AuthController` logs in and exposes protected endpoints. `AuthSecurityTest` proves the 401/403/200 outcomes.

## 🌱 Under the Hood: How It Really Works

#### The filter chain, concretely

Spring Security registers a `FilterChainProxy` (one servlet filter) that delegates to your `SecurityFilterChain` — itself an ordered list of filters (e.g. `BearerTokenAuthenticationFilter` for resource-server JWT, `AuthorizationFilter` for access rules, exception-translation, etc.). The `BearerTokenAuthenticationFilter` extracts the `Authorization: Bearer` token, hands it to the `JwtDecoder`, and on success sets the `Authentication` in the `SecurityContext`; the `AuthorizationFilter` then checks your `authorizeHttpRequests` rules. On failure: the `AuthenticationEntryPoint` returns **401** (not authenticated), the `AccessDeniedHandler` returns **403** (authenticated, not authorized).

```mermaid
flowchart LR
    p["FilterChainProxy"] --> btf["BearerTokenAuthenticationFilter<br/>(extract + decode JWT)"]
    btf --> af["AuthorizationFilter<br/>(authorizeHttpRequests rules)"]
    af --> c["your controller"]
    btf -.->|"no/invalid token"| e1["AuthenticationEntryPoint → 401"]
    af -.->|"rule denies"| e2["AccessDeniedHandler → 403"]
```

*Alt-text: the FilterChainProxy hands the request to the BearerTokenAuthenticationFilter (extracts and decodes the JWT), then the AuthorizationFilter (applies the access rules), then the controller. A missing/invalid token diverts to the AuthenticationEntryPoint (401); a denied rule diverts to the AccessDeniedHandler (403).*

#### `SecurityFilterChain` + the lambda DSL

You expose a `@Bean SecurityFilterChain filterChain(HttpSecurity http)`. The DSL: `authorizeHttpRequests(a -> a.requestMatchers("/api/auth/login").permitAll().requestMatchers("/api/auth/admin").hasRole("ADMIN").anyRequest().authenticated())`, plus `oauth2ResourceServer(...)`, `csrf(...)`, `sessionManagement(...)`. (History: `WebSecurityConfigurerAdapter` was removed in 6.0; `antMatchers`→`requestMatchers`; `authorizeRequests`→`authorizeHttpRequests` — the 🕰️ table below has the full picture.) `hasRole("ADMIN")` requires the authority `ROLE_ADMIN` (the `ROLE_` prefix is added for you).

#### Issuing & validating JWTs (Nimbus)

`NimbusJwtEncoder(new ImmutableSecret<>(secretKey))` signs; `NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(HS256).build()` validates. A token is built from a `JwtClaimsSet` (`issuer`, `subject`, `issuedAt`, `expiresAt`, custom `roles`) + a `JwsHeader`. The decoder checks the **signature** (so a tampered/forged token is rejected) and the **expiry** (so old tokens die). HS256 needs a secret ≥ 256 bits (32 bytes) — ours is.

#### Mapping claims to authorities

A `JwtAuthenticationConverter` + `JwtGrantedAuthoritiesConverter` reads the `roles` claim into Spring authorities. We set `authoritiesClaimName("roles")` and an empty prefix (our claim already carries `ROLE_`), so a token with `roles: ["ROLE_USER"]` grants authority `ROLE_USER` → `hasRole("USER")` works.

#### BCrypt

`BCryptPasswordEncoder.encode("password")` → a `$2a$10$...` string embedding the cost factor and a random 16-byte salt; `matches(raw, hash)` re-derives and compares in constant time. The cost factor (work) makes each guess expensive — you tune it up as hardware improves. *Never* `equals` on a password; *never* store plaintext or a fast unsalted hash (MD5/SHA-1). The stored string is self-describing:

```text
$2a $10 $ <22-char salt> <31-char hash>     ← one 60-char string, no spaces
 │   │         │               └─ the one-way hash of (password + salt)
 │   │         └─ random per-hash salt → same password, different hash every time
 │   └─ cost factor: 2^10 key-expansion rounds — slow by design; raise it as hardware improves
 └─ algorithm version (BCrypt)
```

#### CORS in one minute

**CORS (cross-origin resource sharing)** is the browser-side counterpart to CSRF's story. Browsers enforce the **same-origin policy**: JavaScript on `app.example.com` can't read responses from `api.example.com` unless the *server* opts in with `Access-Control-Allow-*` headers; for non-simple requests the browser first sends a **preflight `OPTIONS`** asking permission. The `cors(Customizer.withDefaults())` line in our chain wires Spring's CORS handling in **early** — before authentication — so a preflight (which carries no token) can be answered instead of dying with a 401. It delegates to whatever `CorsConfigurationSource`/MVC CORS mappings the app defines — we define **none** yet, so no cross-origin access is opened; the real allow-rules arrive when a browser frontend appears (Step 29's gateway). Non-browser clients (curl, our tests) ignore CORS entirely — it's a browser protection.

#### Why CSRF is disabled here

CSRF (cross-site request forgery) tricks a browser into sending a request with the user's **ambient** credentials — i.e. cookies/session sent automatically. A **stateless Bearer-token** API has no cookie/session that rides along automatically (the client must explicitly attach the token), so there's nothing for CSRF to exploit; disabling it is correct (and required, or Spring would block our non-browser clients). If we later add **cookie-based browser sessions**, CSRF protection comes back on for those flows.

#### Security headers

Spring Security sets safe defaults on responses — e.g. `X-Content-Type-Options: nosniff`, `Cache-Control` for protected resources, `X-Frame-Options` (clickjacking). We assert `nosniff` is present.

#### A Spring Security 7 detail (verify, don't guess)

SS7 introduced **authentication factors**: a JWT/bearer authentication also grants a `FACTOR_BEARER` authority alongside your roles. It's an internal marker (useful for step-up auth, Step 17), not a "role" — so our `/me` filters authorities to `ROLE_*` to keep the API contract clean. (We discovered this by *running it* — the raw `/me` initially returned `["FACTOR_BEARER","ROLE_USER"]`.)

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

---

<a id="build"></a>

# C · 🛠️ Build

## 📦 Your Starting Point

You're at **`step-16-start`** (== `step-15-end`). The services exist but are **unsecured**. We add a new `services/auth` module — no Docker/DB (in-memory users for now; DB + OIDC come in Step 17+).

Confirm the start builds:
```bash
./mvnw -q verify   # green, 8 modules, from Step 15
```

## 🛠️ Let's Build It — Step by Step

```mermaid
flowchart TB
    a["0 · auth module (security + oauth2-resource-server)"] --> b["1 · SecurityConfig: filter chain + JwtEncoder/Decoder + BCrypt"]
    b --> c["2 · UserService (in-memory, BCrypt) + JwtService (issue)"]
    c --> d["3 · AuthController: /login, /me, /admin"]
    d --> e["4 · tests: login, 401, 403, 200, BCrypt"]
    e --> f["5 · prove-it artifacts: requests.http · smoke.sh · ADR-0008"]
```

🌳 **Files we'll touch:**
```
services/auth/pom.xml · src/main/resources/application.yml · AuthApplication.java
src/main/java/com/buildabank/auth/
├── security/{SecurityConfig, JwtService}.java
├── user/UserService.java
└── web/{AuthController, AuthDtos}.java
src/test/java/com/buildabank/auth/{AuthSecurityTest, PasswordEncodingTest}.java
pom.xml (+ <module>services/auth</module>) · steps/step-16/{requests.http,smoke.sh} · adr/0008-...md
```

---

### Sub-step 0 of 4 — The auth module · ≈1h 🧭 *(you are here: **module** → config → services → controller → tests)*

🎯 **Goal:** a new module with Spring Security + OAuth2 resource server (for JWT), a main class, and config.

📁 **Location:** `services/auth/pom.xml`, `AuthApplication.java`, `application.yml` + add `<module>services/auth</module>` to the root `pom.xml`.

⌨️ **Code** — the module POM, complete:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- file: services/auth/pom.xml -->
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

The main class — the same shape as every service since Step 1:
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

The config — port, JWT secret/TTL/issuer:
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

Finally, register the module in the root `pom.xml` (after `services/demand-account`):
```xml
        <module>services/auth</module>
```

🔍 **Line-by-line:**
- `starter-security` brings the filter chain + `BCryptPasswordEncoder` + the DSL; `starter-oauth2-resource-server` adds Bearer-JWT validation **and** the Nimbus library we use to *issue* tokens too. All Boot-managed (no versions).
- `starter-web`/`starter-validation`/`starter-actuator` — the usual trio from earlier services (MVC, `@Valid`, `/actuator/health`).
- `spring-security-test` (test scope) — security-aware test support; `spring-boot-webmvc-test` is Boot 4's split-out MockMvc test module.
- `bank.jwt.secret` — a **demo** HMAC secret, env-overridable (`BANK_JWT_SECRET`), ≥ 32 bytes for HS256. `ttl-minutes: 30` and `issuer` feed `JwtService` in sub-step 2.
- `port: 8083` — auth's slot in the port lineup (hello/gateway 8080, cif 8081, demand-account 8082).

✋ **Checkpoint:** `./mvnw -q -pl services/auth dependency:resolve` succeeds.

💾 **Commit:** `git add services/auth pom.xml && git commit -m "build(auth): add Spring Security + OAuth2 resource server module"`

⚠️ **Pitfall:** just adding `starter-security` with no `SecurityFilterChain` secures **everything** with Boot's default login — so the next sub-step's config is what makes it behave as designed.

🛑 **Stopping here?** You have the auth module skeleton (POM, main class, config) resolving its dependencies, committed. Next: Sub-step 1 (`SecurityConfig`); first action: create `services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java`.

---

### Sub-step 1 of 4 — `SecurityConfig`: the filter chain + JWT + BCrypt · ≈2h 🧭 *(module ✅ → **config** → services → controller → tests)*

🎯 **Goal:** the heart of the step — define who can hit what, and the JWT/password machinery.

📁 **Location:** `services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java`

⌨️ **Code** — the complete file:
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
- The constructor takes `bank.jwt.secret` (from `application.yml`, sub-step 0) and keeps its UTF-8 **bytes** — HS256 needs ≥ 32 of them.
- `csrf.disable()` — correct *because* this is a stateless Bearer-token API (no cookie/session to forge).
- `SessionCreationPolicy.STATELESS` — Spring won't create/use an HTTP session; identity comes from the token each request.
- `cors(Customizer.withDefaults())` — **CORS** is the browser's cross-origin opt-in protocol (the server grants foreign origins access via `Access-Control-*` headers). This line wires CORS handling in early so a browser's preflight `OPTIONS` (which carries no token) is answered instead of 401'd; it delegates to a `CorsConfigurationSource`/MVC mappings — we define none yet, so nothing is opened. The real allow-rules arrive with the browser frontend (Step 29's gateway). See "CORS in one minute" in 🌱.
- `authorizeHttpRequests(...)` — the access rules, evaluated **top-down**: `/login` + health are public; `/admin` needs `ROLE_ADMIN`; everything else needs *some* valid identity.
- `oauth2ResourceServer(jwt(...))` — validate the `Authorization: Bearer <jwt>` with the `JwtDecoder`; the converter maps the `roles` claim to authorities.
- `PasswordEncoder` = BCrypt; `JwtEncoder`/`JwtDecoder` = Nimbus over the shared HMAC secret (HS256).
- `secretKey()` — wraps the raw bytes as a `SecretKeySpec` for HMAC-SHA256; both the encoder and decoder derive from this one key (symmetric).
- `jwtAuthConverter()` — reads the `roles` claim with an **empty** authority prefix (our claim already carries `ROLE_`), so `roles: ["ROLE_USER"]` → authority `ROLE_USER` → `hasRole("USER")` works.

💭 **Under the hood:** this builds the `SecurityFilterChain`; the `BearerTokenAuthenticationFilter` validates tokens and sets the `Authentication`, the `AuthorizationFilter` applies the rules (401 if unauthenticated, 403 if unauthorized).

🔮 **Predict:** a request to `/api/auth/me` with **no** `Authorization` header — what status? <details><summary>answer</summary>401 (authentication required). Proven in 🔬.</details>

✋ **Checkpoint:** compiles.

💾 **Commit:** `git add .../security/SecurityConfig.java && git commit -m "feat(auth): stateless JWT SecurityFilterChain + BCrypt + HS256 encoder/decoder"`

⚠️ **Pitfall:** an HMAC secret shorter than 32 bytes fails HS256 at runtime — ours is ≥ 256 bits.

🛑 **Stopping here?** You have the filter chain + JWT/BCrypt beans compiling, committed. Next: Sub-step 2 (`UserService` + `JwtService`); first action: create `services/auth/src/main/java/com/buildabank/auth/user/UserService.java`.

---

### Sub-step 2 of 4 — `UserService` (BCrypt store) + `JwtService` (issue) · ≈2h 🧭 *(… → **services** → …)*

🎯 **Goal:** an in-memory user store with hashed passwords, and a token issuer.

📁 **Location:** `user/UserService.java`, `security/JwtService.java`

⌨️ **Code** — both files, complete:
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

🔍 **Line-by-line:** `passwordEncoder.encode` stores only the BCrypt hash; `matches` verifies an attempt without ever comparing plaintext. The `StoredUser` record keeps username + hash + roles in a `ConcurrentHashMap` (thread-safe, per the 🧵 note). Demo users (`alice`/`password`, `admin`/`admin123`) are seeded at startup with **fake** credentials. `JwtService.issue` builds a claims set (`sub`, `roles`, `exp`, `iat`, `iss`) and signs it → a compact JWT string; `ttlMinutes`/`issuer` come from `application.yml` (sub-step 0), and `ttlSeconds()` feeds the login response's `expiresInSeconds` (sub-step 3).

💭 **Under the hood:** the token's payload is just base64 — readable by anyone (don't put secrets in it). Its *signature* is what proves authenticity; only someone with the secret can produce a matching one.

🔮 **Predict:** call `passwordEncoder.encode("password")` **twice** — same output? Will both verify with `matches`? <details><summary>answer</summary>Different outputs (each `encode` draws a fresh random salt, embedded in the hash) — yet both `matches("password", …)` return true, because `matches` re-derives using the salt inside the stored hash. Sub-step 4's `samePasswordHashesDifferently_dueToSalt` test proves exactly this (see 🔬 §4).</details>

✋ **Checkpoint:** both services compile.

💾 **Commit:** `git add .../user/UserService.java .../security/JwtService.java && git commit -m "feat(auth): in-memory BCrypt user store + JWT issuer"`

⚠️ **Pitfall:** seeding users with plaintext (or encoding once and reusing across restarts) — always hash, and never log the raw password.

🛑 **Stopping here?** You have the BCrypt user store + JWT issuer compiling, committed. Next: Sub-step 3 (`AuthController`); first action: create `services/auth/src/main/java/com/buildabank/auth/web/AuthDtos.java`.

---

### Sub-step 3 of 4 — `AuthController`: login + protected endpoints · ≈2h 🧭 *(… → **controller** → tests)*

🎯 **Goal:** `POST /login` (issue token), `GET /me` (authenticated), `GET /admin` (ADMIN-only).

📁 **Location:** `web/AuthController.java` + `web/AuthDtos.java`

⌨️ **Code** — first the DTOs, complete:
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

Then the controller — complete except `/admin`, which you'll write yourself:
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

    // ✍️ /admin goes here — write it yourself (spec below).
}
```

✍️ **Type-it-yourself — the `/admin` endpoint.** Spec: a `GET /admin` handler returning `Map.of("message", "admin access granted")`. It needs **zero** auth code in the method body — think about why before peeking. <details><summary>solution</summary>

```java
    /** ADMIN-only — reachable only with a token carrying ROLE_ADMIN (else the filter chain returns 403). */
    @GetMapping("/admin")
    public Map<String, String> admin() {
        return Map.of("message", "admin access granted");
    }
```
The **filter chain** already enforced `hasRole("ADMIN")` (sub-step 1's rule) before the method runs — authorization lives in one place, not sprinkled through controller bodies.</details>

🔍 **Line-by-line:** `/login` is public (the filter chain permits it); it checks BCrypt and issues a token or returns 401. `/me` receives the `Authentication` the filter chain built from the validated JWT — `getName()` is the subject (username); we filter authorities to `ROLE_*` (SS7 also grants `FACTOR_BEARER`). `/admin` has no auth code — the **filter chain** already enforced `hasRole("ADMIN")` before the method runs. The DTOs are records: `@NotBlank` + the controller's `@Valid` reject empty credentials before any BCrypt work.

❓ **Knowledge-check:** the `/admin` handler contains zero security code — what stops a `ROLE_USER` token from reaching it? <details><summary>answer</summary>The **security filter chain**: sub-step 1's `requestMatchers("/api/auth/admin").hasRole("ADMIN")` rule is enforced by the `AuthorizationFilter` *before* the controller is invoked, returning 403. Authorization lives in one place (the chain), not in controller bodies.</details>

▶️ **Run & See** (live):
```bash
./mvnw -pl services/auth spring-boot:run
TOKEN=$(curl -s -X POST localhost:8083/api/auth/login -H 'Content-Type: application/json' -d '{"username":"alice","password":"password"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s -H "Authorization: Bearer $TOKEN" localhost:8083/api/auth/me
```
✅ **Expected output** (real run):
```
{"username":"alice","roles":["ROLE_USER"]}
```

✋ **Checkpoint:** login returns a token; `/me` with it returns your identity.

💾 **Commit:** `git add .../web/AuthController.java .../web/AuthDtos.java && git commit -m "feat(auth): login + /me + /admin endpoints"`

⚠️ **Pitfall:** putting `@PreAuthorize` logic in the controller body when the filter chain already enforces it — keep authorization in one place (the chain or method security, not both ad-hoc).

🛑 **Stopping here?** You have a live login→token→`/me` flow working and committed. Next: Sub-step 4 (tests); first action: create `services/auth/src/test/java/com/buildabank/auth/AuthSecurityTest.java`.

---

### Sub-step 4 of 4 — Tests · ≈1.5h 🧭 *(… → **tests**)*

🎯 **Goal:** prove authentication (401), authorization (403), the happy path (200), and BCrypt.

📁 **Location:** `AuthSecurityTest` (real HTTP, RANDOM_PORT) + `PasswordEncodingTest` (pure unit)

⌨️ **Code** — `AuthSecurityTest`, complete except the last two tests (yours to write):
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

    // ✍️ two more tests go here — your turn (spec below).

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

✍️ **Type-it-yourself — the last two tests.** Using the `tokenFor`/`get`/`login` helpers: **(1)** `admin_asAdmin_is200` — log in as `admin`/`admin123` and assert `/api/auth/admin` returns **200**; **(2)** `securityHeadersArePresent` — make any request and assert the `X-Content-Type-Options` header is `nosniff`. <details><summary>solution</summary>

```java
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
```
</details>

And the pure-unit BCrypt test, complete:
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

🔮 **Predict (before running):** if you deleted `/api/auth/login` from the `permitAll()` rule in sub-step 1, which of these tests would fail? <details><summary>answer</summary>Every one that logs in — `/login` would fall into `anyRequest().authenticated()`, so an un-tokened `POST /login` gets 401. `login_withValidCredentials_returnsAToken` fails outright, and every test using `tokenFor(...)` fails with it (no token to fetch). A chicken-and-egg lockout: you can't get a token without a token — which is *why* the login endpoint must be public.</details>

▶️ **Run & See:**
```bash
./mvnw -pl services/auth test
```
✅ **Expected output:**
```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

🔬 **Break-it (the §12.3 mutation):** change `/api/auth/admin` from `hasRole("ADMIN")` to `permitAll()` and rerun — `admin_asNonAdmin_is403` fails (`expected: 403 but was: 200`). Put it back. (See 🔬 §3.)

✋ **Checkpoint:** 9 green tests.

💾 **Commit:** `git add services/auth/src/test && git commit -m "test(auth): authn/authz (401/403/200), JWT flow, BCrypt"`

⚠️ **Pitfall:** `RANDOM_PORT` tests hit a **real server**, so the real filter chain applies — unauthenticated requests really 401; that's the point. (`spring-security-test` is only needed for MockMvc-style tests — `@WithMockUser` and friends — not this class.)

🛑 **Stopping here?** You have 9 green tests and the §12.3 mutation done, committed. Next: Sub-step 5 (prove-it artifacts); first action: create `steps/step-16/requests.http`.

---

### Sub-step 5 — Prove-it artifacts: `requests.http` · `smoke.sh` · ADR-0008 · ≈0.5h 🧭 *(module ✅ → config ✅ → services ✅ → controller ✅ → tests ✅ → **artifacts**)*

🎯 **Goal:** the three artifacts the DoD and files-tree promise — a click-through request file, a one-shot smoke test, and the decision record.

📁 **Location:** `steps/step-16/requests.http`, `steps/step-16/smoke.sh`, `adr/0008-auth-service-and-jwt-security.md`

⌨️ **Code** — `requests.http`, complete (drive it with the IntelliJ/VS Code REST client while `make run-auth` is running):
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

### Decode a JWT (no secret needed to READ it) at https://jwt.io — see the header (HS256),
### and claims sub/roles/exp/iat. The signature is what the server verifies with the shared secret.
```

`smoke.sh`, complete:
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

And ADR-0008 — the four decisions this step baked in (write `adr/0008-auth-service-and-jwt-security.md`; full text in the repo at `step-16-end`, structure below):

| Decision | Choice | Why (short) |
|---|---|---|
| Where to apply security first | A **new `services/auth` module** (not securing demand-account in place) | matches the architecture's service list; securing demand-account would force auth into all 27 of its tests at once. Step 17 makes the other services resource servers. |
| Session vs token | **Stateless JWT** (`STATELESS`, OAuth2 resource server); CSRF off | scales horizontally, no shared session store; a Bearer API has no ambient credentials for CSRF to ride. |
| JWT signing | **HMAC (HS256)** now → asymmetric (JWKS) later | simplest for one issuer+validator; flagged: validators share the secret (can forge) — switch when Step 17+ services validate. |
| User store | **In-memory + BCrypt** (fake demo creds) | Step 16 is about security mechanics, not persistence; DB users + OIDC + MFA arrive Step 17+. |

🔍 **Line-by-line:** `requests.http` walks the exact 200/401/401/403/200 story the tests assert — the same flow, hand-driven. `smoke.sh` is deliberately boring: `set -euo pipefail` + one Maven test run — it exists so *any* future session (or CI) can prove Step 16 in one command. The ADR records *why*, so Step 17 doesn't re-litigate the decisions.

✋ **Checkpoint:** `bash steps/step-16/smoke.sh` finishes with `✅ Step 16 smoke test PASSED` (the real run is pasted in 🔬 §5).

💾 **Commit:** `git add steps/step-16 adr/0008-auth-service-and-jwt-security.md && git commit -m "docs(auth): step-16 requests.http + smoke.sh + ADR-0008"`

⚠️ **Pitfall:** forgetting the shebang/`set -euo pipefail` in `smoke.sh` — without `-e` a failing Maven run still prints PASSED, which is worse than no smoke test.

🛑 **Stopping here?** You have all Step-16 artifacts committed; the build is done. Next: 🎮 Play With It, then the 🔬 Verification Log; first action: `make run-auth`.

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
5. 🧪 **Little experiments:** tamper one character of the token → `/me` → 401 (signature fails); wait past the expiry → 401; change a role claim by hand → 401 (signature no longer matches).

## 🏁 The Finished Result

You're at **`step-16-end`** (== `step-17-start`). The bank has an Identity & Auth service issuing and validating JWTs, with role-based access — **9** green tests.

### ✅ Definition of Done (your self-check)
- [ ] `./mvnw -pl services/auth test` is green with **Tests run: 9**.
- [ ] Login mints a JWT; `/me` needs a valid token (401 without); `/admin` needs ADMIN (403 otherwise).
- [ ] Passwords are BCrypt-hashed; CSRF is off (stateless) with the rationale clear.
- [ ] `bash steps/step-16/smoke.sh` prints `✅ Step 16 smoke test PASSED`.
- [ ] You've committed and tagged `step-16-end`.

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

---

<a id="apply"></a>

# E · 🎓 Apply

## 🚀 Go Deeper (Optional)

<details>
<summary>① Why asymmetric signing (and a JWKS endpoint) is next · +~10 min</summary>

With HMAC, every service that *validates* a token also holds the secret — so any of them could *forge* tokens. When multiple independent services validate (Step 17+), switch to **asymmetric** signing: the auth service signs with a **private** key; each service validates with the **public** key, fetched from a `/.well-known/jwks.json` (**JWKS**) endpoint. Validators can verify but not mint — least privilege. Spring's resource server supports `jwkSetUri` out of the box. (Full **Authorization Server** in Step 41.)
</details>

<details>
<summary>② Method security (@PreAuthorize) vs URL rules · +~5 min</summary>

We authorized by URL (`authorizeHttpRequests`). Spring also offers **method security** (`@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` / `@PostAuthorize` / `@PreFilter`) — authorization expressed on the service/method, closer to the domain and reusable across entry points. Use URL rules for coarse edges and method security for fine-grained domain rules. Step 17 goes deeper.
</details>

<details>
<summary>③ Token revocation & refresh · +~10 min</summary>

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

---

<a id="review"></a>

# F · 🏆 Review

## 🩺 Stuck? Troubleshooting & Fixes

| Symptom | Cause | Fix |
|---|---|---|
| Everything is 401, even `/login` | no `SecurityFilterChain`, or `/login` not `permitAll` | define the chain; `requestMatchers("/api/auth/login").permitAll()`. |
| HS256 fails at startup | HMAC secret < 32 bytes | use a secret of ≥ 32 **bytes** (32+ ASCII characters — multi-byte characters count more). |
| `/admin` returns 403 for the admin too | role/claim mismatch | token `roles` must contain `ROLE_ADMIN`; converter prefix empty (claim already has `ROLE_`). |
| `/me` shows `FACTOR_BEARER` in roles | Spring Security 7 auth-factor authority | filter authorities to `ROLE_*` (we do). |
| Browser/Postman CSRF errors on POST | CSRF enabled on a token API | disable CSRF for the stateless API (re-enable for cookie sessions). |
| Token "works" but tampering isn't caught | reading the JWT without verifying | always validate via the `JwtDecoder` (the resource server does). |
| Reset to known-good | — | `git checkout step-16-end && ./mvnw -pl services/auth test`. |

## 📚 Learn More: Resources & Glossary

- Spring Security reference — **Architecture** (filter chain), **OAuth2 Resource Server (JWT)**, **Password Storage**.
- **RFC 7519** (JWT), **RFC 9457** (errors, Step 13). jwt.io to decode tokens.
- OWASP — Authentication & Password Storage Cheat Sheets.

**Glossary:** **security filter chain / `SecurityFilterChain`** · **authentication (401) vs authorization (403)** · **`authorizeHttpRequests` / `requestMatchers` / `hasRole`** · **JWT (header.claims.signature)** · **resource server / `BearerTokenAuthenticationFilter`** · **`JwtEncoder`/`JwtDecoder` (Nimbus)** · **HMAC (HS256) vs asymmetric (JWKS)** · **claims (`sub`, `roles`, `exp`)** · **BCrypt / salt / cost factor** · **CSRF** · **`SecurityContextHolder` (ThreadLocal)** · **`FACTOR_BEARER`** (SS7).

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
6. JavaScript on `app.example.com` calls our API on another origin — what does the browser do first, and who must allow it? <details><summary>answer</summary>For non-simple requests the browser sends a preflight `OPTIONS` (no token attached); the *server* must opt in with `Access-Control-Allow-*` headers (CORS). The chain handles this before auth — and we define no allow-rules yet (that's Step 29's gateway).</details>

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
