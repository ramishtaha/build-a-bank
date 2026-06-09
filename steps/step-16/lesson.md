# Step 16 · Spring Security Deep I — The Filter Chain, JWT & Password Encoding
### Phase C — Web, APIs & Application Security 🔵 · Step 16 of 67

> *Until now anyone could call any endpoint. That ends here. You'll build the bank's **Identity & Auth**
> service: a Spring Security **filter chain** that authenticates and authorizes every request, **JWTs** issued
> on login and validated on each call, and **BCrypt** for passwords. By the end you can say exactly what
> happens when a request arrives with — and without — a valid token.*

---

<a id="toc"></a>
## 🧭 The Six Movements of This Step

| | Movement | What happens |
|---|---|---|
| **A** | [🧭 Orient](#orient) | 30-second overview · skip-test · cheat card · why it matters · before you start |
| **B** | [🧠 Understand](#understand) | the security filter chain · authn vs authz · JWTs · BCrypt · CSRF/CORS/headers |
| **C** | [🛠️ Build](#build) | the `auth` service: filter chain, JWT encode/decode, BCrypt user store, login + protected endpoints |
| **D** | [🔬 Prove](#prove) | the Verification Log — login→token→401/403/200 over real HTTP, §12.3 mutation |
| **E** | [🎓 Apply](#apply) | go deeper · interview prep · your-turn challenges |
| **F** | [🏆 Review](#review) | troubleshooting · resources · recap, flashcards & what's next |

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

## 🧩 Pattern Spotlight — Stateless JWT Authentication (Resource Server)

> **Problem.** A microservices platform can't rely on server-side sessions: sessions are stateful (sticky load balancing or a shared session store), and every service would need access to them. You need identity that travels *with the request* and that any service can verify independently.

> **Why stateless JWT fits.** A signed JWT carries the identity and is self-contained: any service holding the verification key can validate it (signature + expiry) **without** a session lookup or a call back to the auth service. That scales horizontally (no sticky sessions) and decouples services. Spring models the validating side as an **OAuth2 resource server**.

> **How it works (the mechanism).** Login verifies the password and signs a JWT (`JwtEncoder`). Each later request carries `Authorization: Bearer <jwt>`; the resource-server filter decodes and validates it (`JwtDecoder`: signature via the shared HMAC secret + `exp`), maps the `roles` claim to authorities, and sets the `Authentication` — all stateless. `SessionCreationPolicy.STATELESS` tells Spring not to create/use a session.

> **Alternatives / trade-offs.** **Sessions + cookies** are simple for a single server-rendered app and support easy server-side revocation, but are stateful and CSRF-prone. **Opaque tokens** (a random string the server looks up) give instant revocation but require a lookup per request (back to stateful). JWTs are great for scale/decoupling but are **hard to revoke before expiry** (mitigate with short lifetimes + refresh tokens, Step 17/32). **HMAC vs asymmetric** signing: HMAC (one shared secret) is simplest for one issuer+validator; asymmetric (private key signs, public key/JWKS validates) is the move when many services validate (Step 17/41) — they can verify without being able to forge.

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

### Sub-step 0 of 4 — The auth module 🧭 *(you are here: **module** → config → services → controller → tests)*

🎯 **Goal:** a new module with Spring Security + OAuth2 resource server (for JWT).

📁 **Location:** `services/auth/pom.xml` + add `<module>services/auth</module>` to the root `pom.xml`.

⌨️ **Code** (the security dependencies):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>   <!-- brings spring-security-oauth2-jose (Nimbus) for JWT encode/decode + resource-server -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```
plus `spring-security-test` (test scope) for security-aware tests. All Boot-managed (no versions).

🔍 **Line-by-line:** `starter-security` brings the filter chain + `BCryptPasswordEncoder` + the DSL; `starter-oauth2-resource-server` adds Bearer-JWT validation **and** the Nimbus library we use to *issue* tokens too.

✋ **Checkpoint:** `./mvnw -q -pl services/auth dependency:resolve` succeeds.

💾 **Commit:** `git add services/auth/pom.xml pom.xml && git commit -m "build(auth): add Spring Security + OAuth2 resource server module"`

⚠️ **Pitfall:** just adding `starter-security` with no `SecurityFilterChain` secures **everything** with Boot's default login — so the next sub-step's config is what makes it behave as designed.

---

### Sub-step 1 of 4 — `SecurityConfig`: the filter chain + JWT + BCrypt 🧭 *(module ✅ → **config** → services → controller → tests)*

🎯 **Goal:** the heart of the step — define who can hit what, and the JWT/password machinery.

📁 **Location:** `services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java`

⌨️ **Code** (the filter chain; full file in the repo):
```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())                                  // stateless token API → no CSRF
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/auth/login", "/actuator/health").permitAll()   // public
            .requestMatchers("/api/auth/admin").hasRole("ADMIN")                   // authZ: role
            .anyRequest().authenticated())                                         // authN: any valid identity
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
    return http.build();
}

@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
@Bean JwtEncoder jwtEncoder() { return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey())); }
@Bean JwtDecoder jwtDecoder() { return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(HS256).build(); }
```

🔍 **Line-by-line:**
- `csrf.disable()` — correct *because* this is a stateless Bearer-token API (no cookie/session to forge).
- `SessionCreationPolicy.STATELESS` — Spring won't create/use an HTTP session; identity comes from the token each request.
- `authorizeHttpRequests(...)` — the access rules, evaluated **top-down**: `/login` + health are public; `/admin` needs `ROLE_ADMIN`; everything else needs *some* valid identity.
- `oauth2ResourceServer(jwt(...))` — validate the `Authorization: Bearer <jwt>` with the `JwtDecoder`; the converter maps the `roles` claim to authorities.
- `PasswordEncoder` = BCrypt; `JwtEncoder`/`JwtDecoder` = Nimbus over the shared HMAC secret (HS256).

💭 **Under the hood:** this builds the `SecurityFilterChain`; the `BearerTokenAuthenticationFilter` validates tokens and sets the `Authentication`, the `AuthorizationFilter` applies the rules (401 if unauthenticated, 403 if unauthorized).

🔮 **Predict:** a request to `/api/auth/me` with **no** `Authorization` header — what status? <details><summary>answer</summary>401 (authentication required). Proven in 🔬.</details>

✋ **Checkpoint:** compiles.

💾 **Commit:** `git add .../security/SecurityConfig.java && git commit -m "feat(auth): stateless JWT SecurityFilterChain + BCrypt + HS256 encoder/decoder"`

⚠️ **Pitfall:** an HMAC secret shorter than 32 bytes fails HS256 at runtime — ours is ≥ 256 bits.

---

### Sub-step 2 of 4 — `UserService` (BCrypt store) + `JwtService` (issue) 🧭 *(… → **services** → …)*

🎯 **Goal:** an in-memory user store with hashed passwords, and a token issuer.

📁 **Location:** `user/UserService.java`, `security/JwtService.java`

⌨️ **Code** (the essence):
```java
// UserService — BCrypt, never plaintext
private void register(String username, String rawPassword, List<String> roles) {
    users.put(username, new StoredUser(username, passwordEncoder.encode(rawPassword), roles));
}
public Optional<StoredUser> authenticate(String username, String rawPassword) {
    StoredUser u = users.get(username);
    return (u != null && passwordEncoder.matches(rawPassword, u.passwordHash())) ? Optional.of(u) : Optional.empty();
}

// JwtService — sign a token
public String issue(String username, List<String> roles) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).issuedAt(now)
        .expiresAt(now.plus(Duration.ofMinutes(ttlMinutes))).subject(username).claim("roles", roles).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
}
```

🔍 **Line-by-line:** `passwordEncoder.encode` stores only the BCrypt hash; `matches` verifies an attempt without ever comparing plaintext. `JwtService.issue` builds a claims set (`sub`, `roles`, `exp`, `iat`) and signs it → a compact JWT string. Demo users (`alice`/`password`, `admin`/`admin123`) are seeded at startup with **fake** credentials.

💭 **Under the hood:** the token's payload is just base64 — readable by anyone (don't put secrets in it). Its *signature* is what proves authenticity; only someone with the secret can produce a matching one.

✋ **Checkpoint:** both services compile.

💾 **Commit:** `git add .../user/UserService.java .../security/JwtService.java && git commit -m "feat(auth): in-memory BCrypt user store + JWT issuer"`

⚠️ **Pitfall:** seeding users with plaintext (or encoding once and reusing across restarts) — always hash, and never log the raw password.

---

### Sub-step 3 of 4 — `AuthController`: login + protected endpoints 🧭 *(… → **controller** → tests)*

🎯 **Goal:** `POST /login` (issue token), `GET /me` (authenticated), `GET /admin` (ADMIN-only).

📁 **Location:** `web/AuthController.java` + `web/AuthDtos.java`

⌨️ **Code:**
```java
@PostMapping("/login")
public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return users.authenticate(request.username(), request.password())
            .map(u -> ResponseEntity.ok(new TokenResponse(jwt.issue(u.username(), u.roles()), jwt.ttlSeconds())))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());   // bad creds → 401
}

@GetMapping("/me")
public MeResponse me(Authentication authentication) {
    List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_")).sorted().toList();   // filter out SS7 FACTOR_* authorities
    return new MeResponse(authentication.getName(), roles);
}

@GetMapping("/admin")
public Map<String, String> admin() { return Map.of("message", "admin access granted"); }
```

🔍 **Line-by-line:** `/login` is public (the filter chain permits it); it checks BCrypt and issues a token or returns 401. `/me` receives the `Authentication` the filter chain built from the validated JWT — `getName()` is the subject (username); we filter authorities to `ROLE_*` (SS7 also grants `FACTOR_BEARER`). `/admin` has no auth code — the **filter chain** already enforced `hasRole("ADMIN")` before the method runs.

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

---

### Sub-step 4 of 4 — Tests 🧭 *(… → **tests**)*

🎯 **Goal:** prove authentication (401), authorization (403), the happy path (200), and BCrypt.

📁 **Location:** `AuthSecurityTest` (real HTTP, RANDOM_PORT) + `PasswordEncodingTest` (pure unit)

⌨️ **Code** (the security assertions; full files in the repo):
```java
assertThat(login("alice", "password").statusCode()).isEqualTo(200);     // valid → token
assertThat(login("alice", "WRONG").statusCode()).isEqualTo(401);        // bad password → 401
assertThat(get("/api/auth/me", null).statusCode()).isEqualTo(401);      // no token → 401 (authN)
assertThat(get("/api/auth/admin", tokenFor("alice","password")).statusCode()).isEqualTo(403);  // wrong role → 403 (authZ)
assertThat(get("/api/auth/admin", tokenFor("admin","admin123")).statusCode()).isEqualTo(200);  // ADMIN → 200
assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");        // header
```

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

⚠️ **Pitfall:** `@SpringBootTest` with `spring-security-test` applies the real filter chain — so unauthenticated requests really 401; that's the point.

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
<summary>① Why asymmetric signing (and a JWKS endpoint) is next</summary>

With HMAC, every service that *validates* a token also holds the secret — so any of them could *forge* tokens. When multiple independent services validate (Step 17+), switch to **asymmetric** signing: the auth service signs with a **private** key; each service validates with the **public** key, fetched from a `/.well-known/jwks.json` (**JWKS**) endpoint. Validators can verify but not mint — least privilege. Spring's resource server supports `jwkSetUri` out of the box. (Full **Authorization Server** in Step 41.)
</details>

<details>
<summary>② Method security (@PreAuthorize) vs URL rules</summary>

We authorized by URL (`authorizeHttpRequests`). Spring also offers **method security** (`@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` / `@PostAuthorize` / `@PreFilter`) — authorization expressed on the service/method, closer to the domain and reusable across entry points. Use URL rules for coarse edges and method security for fine-grained domain rules. Step 17 goes deeper.
</details>

<details>
<summary>③ Token revocation & refresh</summary>

A JWT is valid until it expires — you can't easily revoke it mid-life (no server lookup). Mitigations: **short** access-token lifetimes (minutes) + a **refresh token** (longer-lived, revocable, used to mint new access tokens), or a denylist of revoked token ids (reintroduces some state). The access/refresh split is standard OAuth2 (Step 17/32 for the frontend flow).
</details>

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
| HS256 fails at startup | HMAC secret < 32 bytes | use a ≥ 256-bit (≥ 32-char) secret. |
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
