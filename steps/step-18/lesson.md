# Step 18 · Secure Coding & Threat Modeling — DevSecOps Shift-Left
### Phase C — Web, APIs & Application Security 🔵 · Step 18 of 67 · **End of Phase C** 🎓

> *You've built a real bank: customers, a ledger, transfers, a gateway, identity, resource servers. Now,
> before adding distributed messaging, you do what every serious team does — you **stop and ask "how would
> someone attack this?"** in a structured way. This step is **STRIDE threat modeling** of the bank you built,
> a walkthrough of the **OWASP Top 10 + API Security Top 10** against your own code, and the **secure-by-default
> hardening + security tests** the model demands. You'll find a real, critical bug in your own design (BOLA) —
> and learn why a senior engineer **records and schedules it** rather than half-fixing it in a hurry.*

---

<a id="toc"></a>
## 🧭 The Six Movements of This Step

| | Movement | What happens |
|---|---|---|
| **A** | [🧭 Orient](#orient) | 30-second overview · skip-test · cheat card · why it matters · before you start |
| **B** | [🧠 Understand](#understand) | what threat modeling is · STRIDE · trust boundaries & DFDs · OWASP Top 10 / API Top 10 · BOLA |
| **C** | [🛠️ Build](#build) | write the threat model · harden the edge (headers + deny-by-default CORS) · injection-safety test · hardening test |
| **D** | [🔬 Prove](#prove) | the Verification Log — real test runs, the live CORS-403, the §12.3 mutation, smoke.sh |
| **E** | [🎓 Apply](#apply) | go deeper · interview prep · your-turn challenges (incl. the Phase-C capstone) |
| **F** | [🏆 Review](#review) | troubleshooting (the CORS two-beans gotcha) · resources · recap, flashcards & what's next |

---

<a id="orient"></a>

# A · 🧭 Orient

## 📋 This Step in 30 Seconds

| | |
|---|---|
| **Title** | Secure coding & threat modeling — STRIDE, OWASP Top 10 / API Security Top 10, secure defaults, security tests |
| **Step** | 18 of 67 · **Phase C — Web, APIs & Application Security** 🔵 · **the Phase-C finale** |
| **Effort** | ≈ 14 hours focused. About half is *thinking* (the threat model — the highest-leverage hour you'll spend), half is code (hardening + tests). Experienced security-minded learners can skim to ~3h. |
| **What you'll run this step** | **JVM + Maven**; **🐳 Docker** for the cif + demand-account Testcontainers tests. One command: `./mvnw -pl services/cif,services/demand-account -am verify` (or `bash steps/step-18/smoke.sh`). No new service. |
| **Buildable artifact** | **`security/threat-model.md`** (STRIDE DFD + trust boundaries + attack trees + OWASP×2 walkthrough + capstone) and **`security/risk-register.md`** (prioritized open risks). **demand-account hardening**: explicit security headers + **deny-by-default CORS** (`SecurityHardeningTest`, +3 tests → 34). **cif**: `SqlInjectionSafetyTest` proving parameterized queries are injection-safe, with a vulnerable contrast (+3 → 24). ADR-0010. `step-18-start == step-17-end`. |
| **Verification tier** | 🔴 **Full** — touches a security path (headers/CORS) and adds critical-path security tests. `./mvnw verify` green + the new behavior proven by real output + **§12.3 mutation** (permissive CORS → the deny-by-default test fails → revert) + clean-room + `smoke.sh`. |
| **Depends on** | **[Step 17](../step-17/lesson.md)** (auth/resource servers — the access controls we audit), **[Step 14](../step-14/lesson.md)** (signed webhooks), **[Step 13](../step-13/lesson.md)** (ProblemDetail), **[Step 8/10](../step-08/lesson.md)** (cif + Testcontainers). **+ Docker.** |

By the end you will be able to **threat-model a system with STRIDE**, walk the **OWASP Top 10 and API Security Top 10** against real code, recognize **BOLA** (the #1 API risk) and explain why ours is open, and ship **secure-by-default** edge hardening with **tests that prove injection-safety, security headers, and locked-down CORS.**

### ⏭️ Can You Skip This Step? (5-minute self-check)

If you can confidently do **all** of this, skim the 🛠️ Build and jump to **[Step 19 — Messaging & events](../step-19/lesson.md)**.

- [ ] I can explain **STRIDE** and run it **per-element** over a data-flow diagram with **trust boundaries**.
- [ ] I can name the **OWASP API Security Top 10 #1 (BOLA)** and spot it in a transfer endpoint that takes an account id from the request.
- [ ] I can configure **secure response headers** and a **deny-by-default CORS** policy in Spring Security — and explain why CORS is *not* an access control.
- [ ] I can prove a query is **injection-safe** with a test (and explain why parameterization beats escaping/blocklists).
- [ ] I know why a senior engineer **records a critical finding in a risk register and schedules a proper fix** rather than bolting on a half-fix.

> [!TIP]
> Not 100%? Stay — this is the step that turns "I write Spring apps" into "I reason about how they get attacked." "What's BOLA?", "STRIDE the login flow", and "how do you stop SQL injection — really?" are interview staples.

## 📇 Cheat Card

> **What this step delivers (one sentence):** a STRIDE threat model of the bank that surfaces a real critical bug (BOLA), the secure-by-default edge hardening it demands (security headers + deny-by-default CORS), and tests proving injection-safety / headers / CORS — with the BOLA fix honestly tracked, not faked.

**Key commands** (Windows uses `.\mvnw.cmd`):

```bash
# Run the Step-18 security tests (needs Docker)
./mvnw -pl services/cif,services/demand-account -am test -Dtest='SqlInjectionSafetyTest,SecurityHardeningTest'
# Full proof for this step
bash steps/step-18/smoke.sh
# See the headers + CORS on the running service
curl -i http://localhost:8082/api/accounts/ACC-A
curl -i -X OPTIONS http://localhost:8082/api/v1/transfers -H "Origin: https://evil.example" -H "Access-Control-Request-Method: POST"
```

**The headline diagram — STRIDE per element across a trust boundary:**

```
Internet  │  Edge/DMZ   │      Internal (trusted)        │  Data
  client ─┼─► gateway ──┼─► auth / demand-account / cif ─┼─► Postgres
          │             │                                │
   TB1 ───┘     TB2 ────┘            TB4 (JDBC) ──────────┘
  (authn)     (authz, BOLA!)        (parameterized SQL)
```

**The one sentence to remember:** *Threat modeling's job is to **find and prioritize** risks (and BOLA is ours); you ship the cheap, complete fixes now and **track** the deep ones — you never pretend an open risk is closed.*

## 🎯 Why This Matters

A bank that passes every functional test can still be trivially robbed if any authenticated user can transfer from **someone else's** account. Functional tests prove "it does what I asked"; **threat modeling** asks "what else can it do that I *didn't* ask?" — and that question is where breaches live. In interviews and on the job, "walk me through how you'd threat-model this" and "what's BOLA / how do you prevent SQL injection" separate engineers who ship features from engineers trusted with money and PII.

## ✅ What You'll Be Able to Do

- Produce a **STRIDE-per-element** threat model with a DFD, trust boundaries, and attack trees.
- Map findings to the **OWASP Top 10 (2021)** and **API Security Top 10 (2023)**.
- Identify and explain **BOLA / IDOR** and why authentication ≠ authorization.
- Harden a Spring service with **security headers** and **deny-by-default CORS**, and **test** it.
- Prove **injection-safety** with a side-by-side vulnerable-vs-parameterized contrast.
- Keep a **risk register**: record, prioritize, own, and schedule what you don't fix today.

## 🧰 Before You Start

- **Prereqes:** the bank through Step 17 builds green (`git describe` → `step-17-end`); Docker running.
- **Connects to what you know:** you'll audit the **resource-server auth** (Step 17), the **signed webhooks** (Step 14), the **ProblemDetail** errors (Step 13), and the **parameterized JPA** (Steps 8–10). This step doesn't add a service — it *examines* the ones you have.
- **Depends on:** Steps **17, 14, 13, 8/10**. **+ Docker.**

---

<a id="understand"></a>

# B · 🧠 Understand

## 🧠 The Big Idea — threat modeling is "design review for attackers"

Threat modeling answers four questions (Adam Shostack's framing):

1. **What are we building?** → a model: a **data-flow diagram (DFD)** of processes, data stores, external entities, and the flows between them.
2. **What can go wrong?** → **STRIDE**, applied to every element and every flow.
3. **What are we going to do about it?** → mitigations, each shipped *or* recorded in a **risk register** with an owner and a date.
4. **Did we do a good job?** → review it; revisit when the system changes.

It's **shift-left**: doing this on a diagram costs minutes; discovering the same flaw in production costs a breach. The single most important artifact is the **trust boundary** — the line where data moves from less-trusted to more-trusted. *Every boundary crossing is a checkpoint that must authenticate, authorize, and validate.*

```mermaid
flowchart LR
    subgraph internet["🌐 Internet (UNTRUSTED)"]
        client["Client / partner"]
    end
    subgraph edge["🚪 Edge"]
        gw["Gateway :8080"]
    end
    subgraph internal["🏦 Internal (trusted today)"]
        auth["auth :8083"]
        da["demand-account :8082"]
        cif["cif :8081 (PII)"]
    end
    db[("Postgres")]
    client -->|"TB1 authn"| gw
    gw -->|"TB2 authz"| da
    gw --> auth
    gw --> cif
    da -->|"TB4 JDBC"| db
```

> 🔬 **Break-it-on-purpose (thought experiment):** pick the `da → Postgres` flow. If a user controls part of a query string, what crosses TB4? If it's *data* (a bound parameter) → safe. If it's *code* (concatenated SQL) → injection. That single distinction is OWASP A03.

## 🧩 Pattern Spotlight — STRIDE

**Problem:** "think of everything that could go wrong" is unbounded and you'll miss categories. **STRIDE** is a checklist of six threat *categories*, each the violation of a security property:

| Letter | Threat | Violates | Bank example |
|---|---|---|---|
| **S** | Spoofing | Authentication | Forge a JWT; pretend to be Alice |
| **T** | Tampering | Integrity | Alter an amount; corrupt the ledger |
| **R** | Repudiation | Non-repudiation | "I never made that transfer" |
| **I** | Information disclosure | Confidentiality | Read someone else's balance/PII |
| **D** | Denial of service | Availability | Flood transfers; exhaust the pool |
| **E** | Elevation of privilege | Authorization | USER performs an ADMIN action; **move another user's money** |

**Why it fits:** applied **per element** (each process, store, flow), STRIDE turns "what could go wrong?" into six concrete questions you ask of each box and arrow — systematic, repeatable, reviewable. **Alternatives:** *attack trees* (goal-first, good for depth on one asset — we use them too), *PASTA*/*LINDDUN* (heavier / privacy-focused). STRIDE-per-element is the best first tool.

## 🌱 Under the Hood: the controls you're auditing

- **Authentication vs authorization.** Step 17 gave us **authentication** (a valid JWT) and *function-level* authorization (`@PreAuthorize` on admin ops). It did **not** give us **object-level** authorization — "does *this* user own *this* account?". That gap is **BOLA**.
- **Security headers** are response headers the browser obeys: `X-Content-Type-Options: nosniff` (don't guess MIME types → blocks some XSS), `X-Frame-Options: DENY` (don't let other sites frame us → anti-clickjacking), `Referrer-Policy: no-referrer` (don't leak our URLs to third parties), `Strict-Transport-Security` (force HTTPS — only emitted over TLS). Spring Security writes some by default; we make them explicit and add Referrer-Policy + HSTS.
- **CORS** (Cross-Origin Resource Sharing) is a **browser** mechanism: it asks our server "may a page from origin X call you?" via a *preflight* `OPTIONS`. **Deny-by-default** means we answer "no" unless X is explicitly allow-listed. ⚠️ CORS is a **guardrail for browsers, not an access control** — a non-browser client (curl, another server) ignores it entirely. The real gate is the JWT.

## 🛡️ Security Lens: BOLA, the #1 API risk — and it's in our code

**Broken Object Level Authorization** (OWASP API1:2023; also called IDOR) is when an endpoint takes an object identifier from the request and acts on it **without checking the caller is entitled to that object**. Look at our transfer:

```java
// TransferController.transferV1 — from is taken straight from the body, never checked against the caller
idempotentTransfers.transfer(idempotencyKey, request.from(), request.to(), request.amount(), ...);
```

Any user with **any** valid token can set `from` to **anyone's** account and drain it. Same for `GET /api/accounts/{n}` — read any balance. **This is a real, critical bug in the bank you built.** We will *not* sweep it under the rug, and we will *not* rush a half-fix. See §C and the capstone for why we **record and schedule** it.

## 🕰️ Then vs. Now

Old advice for injection was "escape user input" or "blocklist bad characters" — fragile and bypassable. **Now**: use **parameterized queries / prepared statements** (and ORMs that do this for you — Spring Data binds parameters), so user input is *always data, never code*. Old CSRF advice ("add tokens everywhere") changes for **stateless token APIs**: with no cookies, there's no CSRF vector, so we disable CSRF **deliberately** — and must re-enable it the moment any cookie-based auth appears.

---

<a id="build"></a>

# B→C bridge: 🗺️ what we'll build

```mermaid
flowchart TD
    A["1 · Threat model\nsecurity/threat-model.md"] --> B["2 · Risk register\nsecurity/risk-register.md"]
    B --> C["3 · Harden edge\nSecurityConfig: headers + deny-by-default CORS"]
    C --> D["4 · Test hardening\nSecurityHardeningTest (demand-account)"]
    B --> E["5 · Prove injection-safe\nSqlInjectionSafetyTest (cif)"]
    C --> F["6 · ADR-0010 + capstone"]
```

🌳 **Files we'll touch**

```
security/
  threat-model.md          (new) STRIDE DFD, trust boundaries, attack trees, OWASP×2, capstone
  risk-register.md         (new) prioritized open risks (R-001 BOLA …)
adr/
  0010-threat-model-and-secure-defaults.md   (new)
services/demand-account/
  src/main/java/.../web/SecurityConfig.java          (edit) headers + deny-by-default CORS
  src/main/resources/application.yml                 (edit) app.security.cors.allowed-origins
  src/test/java/.../web/SecurityHardeningTest.java    (new) +3 tests
services/cif/
  src/test/java/.../domain/SqlInjectionSafetyTest.java (new) +3 tests
steps/step-18/{lesson.md, requests.http, smoke.sh}
```

<a id="build"></a>

# C · 🛠️ Let's Build It — Step by Step

## 📦 Your Starting Point

`step-18-start == step-17-end`: the whole bank builds green (`./mvnw verify`, 9 modules). What's green: auth (RS256+JWKS), demand-account as a resource server, cif on Postgres. What you'll add: the threat model + register, two test classes, and the edge hardening they justify.

---

## Sub-step 1 — Model the system & run STRIDE (`security/threat-model.md`)

🎯 **Goal.** Produce the model and the per-element STRIDE analysis. This is *thinking made durable* — the highest-leverage part of the step.

📁 **Location.** New file `security/threat-model.md` (full content in the repo). It contains: the asset list, the DFD, a **trust-boundary table**, **STRIDE-per-element** tables (demand-account first — it's the money), two **attack trees**, the **OWASP Top 10** and **API Security Top 10** walkthroughs, the secure-defaults checklist, and the **🎓 capstone** STRIDE of the transfer feature.

🔍 **The method, concretely.** For each element, ask all six STRIDE letters and record one of ✅ mitigated / 🟡 partial / 🔴 **open**. The two 🔴 rows on demand-account both trace to the same cause:

```
I (Information disclosure)  read any account     → BOLA → R-001
E (Elevation of privilege)  move any account's $  → BOLA → R-001
```

🔮 **Predict:** before reading on — which OWASP **API** Top-10 item is our worst, and is it *authentication* or *authorization*?

<details><summary>Answer</summary>**API1:2023 BOLA** — an **authorization** failure (object-level). Authentication is fine; we just never check *ownership*.</details>

💭 **Under the hood — why record, don't rush-fix.** A correct BOLA fix is an **ownership model**: add `owner_subject` to `account`, set it from `jwt.getSubject()` at open time, and enforce it on every money/read path — a schema migration + service-layer authz + reworking every test fixture that today uses arbitrary account numbers. That's a *focused step of its own*. A half-enforced fix is **worse** than a tracked open risk because it manufactures false confidence. So we log **R-001 (CRITICAL)** with a fully specified remediation and an owning step, note the interim control (it's authenticated-user-only, not anonymous), and move on. **That is what senior shift-left looks like.**

✋ **Checkpoint.** `security/threat-model.md` and `security/risk-register.md` exist; the register's top item is **R-001 BOLA**, CRITICAL, with a concrete fix and an owning step.

💾 **Commit (after the whole step builds):** `docs(security): STRIDE threat model + risk register (Step 18)`

⚠️ **Pitfall.** A threat model that says "everything's fine" is a *bad* threat model — it means you didn't look hard enough. Ours names a critical bug in our own code. Good.

---

## Sub-step 2 — Harden the edge: security headers (`SecurityConfig.java`)

🎯 **Goal.** Make demand-account emit explicit secure response headers (the model called for them under A05 Security Misconfiguration).

📁 **Location.** `services/demand-account/src/main/java/com/buildabank/account/web/SecurityConfig.java` — add a `.headers(...)` block to the filter chain.

⌨️ **Code (edit — before → after the `.cors(...)` line):**

```java
// services/demand-account/src/main/java/com/buildabank/account/web/SecurityConfig.java
.headers(headers -> headers                                    // Step 18: explicit secure response headers
        .frameOptions(frame -> frame.deny())                   // X-Frame-Options: DENY — no framing (anti-clickjacking)
        .contentTypeOptions(Customizer.withDefaults())         // X-Content-Type-Options: nosniff — no MIME sniffing
        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))  // Referrer-Policy: no-referrer — don't leak URLs
        .httpStrictTransportSecurity(hsts -> hsts             // HSTS — force HTTPS (only emitted over TLS)
                .includeSubDomains(true).maxAgeInSeconds(31_536_000)))
```

🔍 **Line-by-line.** `headers(...)` configures the `HeaderWriterFilter`, which writes these on **every** response (even a 401). `frameOptions.deny()` → browsers refuse to render us in a frame. `contentTypeOptions` → browsers won't second-guess our `Content-Type`. `referrerPolicy(NO_REFERRER)` → outbound links don't leak our URL. `httpStrictTransportSecurity` → tells browsers "always use HTTPS for a year" — but Spring only emits it over an actual TLS connection, so you won't see it in plain-HTTP tests (that's correct, not a bug). Import: `org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy`.

💭 **Under the hood.** These are *belt-and-suspenders* for browser clients; they don't replace authn/authz. They're cheap, complete, and worth shipping today.

🔮 **Predict:** will these headers appear on a **401** (unauthenticated) response? <details><summary>Answer</summary>**Yes** — `HeaderWriterFilter` runs early in the chain, before authorization rejects the request. We rely on this in the test.</details>

---

## Sub-step 3 — Harden the edge: deny-by-default CORS

🎯 **Goal.** No browser origin may call us unless explicitly allow-listed.

⌨️ **Code (the `.cors(...)` line + a new bean):**

```java
// in filterChain(...)
.cors(Customizer.withDefaults())   // resolves the corsConfigurationSource bean BY NAME (Step 18, deny-by-default)

// new @Bean
@Bean
CorsConfigurationSource corsConfigurationSource(
        @Value("${app.security.cors.allowed-origins:}") List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins.stream().filter(o -> !o.isBlank()).toList());  // empty ⇒ deny all
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
    config.setMaxAge(Duration.ofHours(1));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

And in `application.yml`:

```yaml
app:
  security:
    cors:
      allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:}   # empty ⇒ no browser origin allowed
```

🔍 **Line-by-line.** The allow-list comes from config and **defaults to empty** → `setAllowedOrigins([])` → every cross-origin preflight is rejected. Add the React app later with `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173`. Server-to-server callers send **no `Origin`** header, so CORS never engages — our existing integration tests (HttpClient, no Origin) are unaffected.

⚠️ **Pitfall — the two-beans trap (this one bit me; verbatim in 🩺).** Do **not** inject `CorsConfigurationSource` *by type* into `filterChain(HttpSecurity, CorsConfigurationSource)`. Spring MVC's `mvcHandlerMappingIntrospector` *also* implements `CorsConfigurationSource`, so there are **two** beans and the context fails: *"expected single matching bean but found 2: corsConfigurationSource, mvcHandlerMappingIntrospector."* The fix is `.cors(Customizer.withDefaults())`, which Spring Security resolves **by the bean name** `corsConfigurationSource`.

✋ **Checkpoint.** `grep frameOptions` and `grep corsConfigurationSource` both hit in `SecurityConfig.java`.

---

## Sub-step 4 — Test the hardening (`SecurityHardeningTest`, demand-account)

🎯 **Goal.** Prove the headers appear and the deny-by-default CORS rejects an un-listed origin — and that authn is still the real gate.

📁 **Location.** `services/demand-account/src/test/java/com/buildabank/account/web/SecurityHardeningTest.java` (full file in the repo). It's a `@WebMvcTest(TransferController.class)` slice importing `SecurityConfig`, mocking the services + `JwtDecoder`, using the `jwt()` post-processor where a request must reach the controller.

⌨️ **The three tests (essence):**

```java
@Test void everyResponseCarriesHardenedSecurityHeaders() // 200 + X-Content-Type-Options/X-Frame-Options/Referrer-Policy
@Test void crossOriginPreflightFromAnUnlistedOriginIsRejected() // OPTIONS + evil Origin → 403, no Access-Control-Allow-Origin
@Test void unauthenticatedMoneyRequestIs401() // no token → 401 (CORS/headers are defense-in-depth, not authZ)
```

🔮 **Predict:** the preflight from `https://evil.example` returns status **___** and the `Access-Control-Allow-Origin` header is **___**. <details><summary>Answer</summary>**403**, and **absent** (not reflected). Verified — see the Verification Log.</details>

---

## Sub-step 5 — Prove injection-safety with a contrast (`SqlInjectionSafetyTest`, cif)

🎯 **Goal.** Prove cif's queries are parameterized (injection-safe) **and** that the proof has teeth, by running the same payload through a vulnerable concatenated query.

📁 **Location.** `services/cif/src/test/java/com/buildabank/cif/domain/SqlInjectionSafetyTest.java` (full file in the repo) — a `@DataJpaTest` against real Postgres (Testcontainers), same harness as `CustomerRepositoryTest`.

⌨️ **The crux — the contrast test:**

```java
// Parameterized repo: payload is DATA → matches 0
long viaParameterized = repository.findByCustomerNumber("' OR '1'='1").map(c -> 1L).orElse(0L);  // 0
// ❗ ANTI-PATTERN (test-only): concatenated SQL → payload is CODE → matches every row
long viaConcat = ((Number) em.createNativeQuery(
        "SELECT count(*) FROM customer WHERE customer_number = '" + "' OR '1'='1" + "'")
        .getSingleResult()).longValue();  // 1
```

🔍 **Why this is the whole lesson.** Same payload, two code paths, **opposite results** (0 vs 1). The parameterized query binds `' OR '1'='1` as a value compared to `customer_number` → no match. The concatenated query splices it into the SQL text → `… = '' OR '1'='1'` → always-true → every row. **Use bound parameters; never build SQL by concatenation.**

🔬 **Break-it-on-purpose:** the destructive-payload test (`'; DROP TABLE customer; --`) asserts the table *survives* — proof the input was never parsed as SQL.

💾 **Commit (after build is green):** `feat(cif,demand-account): Step 18 secure coding — injection-safety test, edge hardening (headers+CORS), threat model`

---

## 🎮 Play With It

Run the live service and *see* the defenses (full set in [`requests.http`](requests.http)):

```bash
# headers on any response (even a 401):
curl -i http://localhost:8082/api/accounts/ACC-A
#   → HTTP/1.1 401 ... X-Content-Type-Options: nosniff ... X-Frame-Options: DENY ... Referrer-Policy: no-referrer

# deny-by-default CORS — preflight from an un-listed origin:
curl -i -X OPTIONS http://localhost:8082/api/v1/transfers \
     -H "Origin: https://evil.example" -H "Access-Control-Request-Method: POST"
#   → HTTP/1.1 403 ... (no Access-Control-Allow-Origin)
```

🧪 **Little experiments:**
- Set `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173`, restart, re-run the preflight with that Origin → now allowed (ACAO present). Try a *different* origin → still 403.
- Log in for a token, open `ACC-A`, then `GET /api/accounts/ACC-A` with the token → 200. Now read `ACC-B` you don't own → **also 200** 😱 — that's **BOLA (R-001)** live. Feel why it's the #1 finding.

## 🏁 The Finished Result

`step-18-end`: the bank still builds green (9 modules), now with a versioned threat model + risk register, hardened edge headers + CORS on the money service, and tests proving injection-safety, headers, and CORS. **✅ Learner Definition of Done:** you can explain STRIDE + BOLA, `./mvnw verify` is green, `bash steps/step-18/smoke.sh` passes, and you've committed/tagged `step-18-end`.

---

<a id="prove"></a>

# D · 🔬 Prove It Works — Verification Log

> **Tier: 🔴 Full.** Security path (headers/CORS) + critical-path security tests. Evidence below is real, pasted output (Docker/Testcontainers used). Per **§12.8**: the BOLA finding (R-001) is *intentionally left open and tracked* — no test claims it's fixed.

**1 · Injection-safety (cif) — `SqlInjectionSafetyTest`, real Postgres (Testcontainers):**

```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.177 s -- in com.buildabank.cif.domain.SqlInjectionSafetyTest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0    ← cif module total (21 prior + 3 new)
```
The contrast test passes: the `' OR '1'='1` payload matched **0** rows via the parameterized repository and **1** via the concatenated query — the delta proves both safety and that the test has teeth.

**2 · Edge hardening (demand-account) — `SecurityHardeningTest` + full module green:**

```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.407 s -- in com.buildabank.account.web.SecurityHardeningTest
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0    ← demand-account total (31 prior + 3 new)
[INFO] BUILD SUCCESS
```

**Real headers observed on a response (captured from the test run):**

```
X-Content-Type-Options:"nosniff", X-XSS-Protection:"0",
Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0",
X-Frame-Options:"DENY", Referrer-Policy:"no-referrer"
```

**3 · §12.3 Mutation sanity-check (prove the deny-by-default CORS test has teeth).** Temporarily made CORS permissive (`config.setAllowedOrigins(List.of("*"))`) and re-ran:

```
Headers = [... Access-Control-Allow-Origin:"*", Access-Control-Allow-Methods:"GET,POST,PUT,DELETE,OPTIONS" ...]
[ERROR] SecurityHardeningTest.crossOriginPreflightFromAnUnlistedOriginIsRejected:73 Status expected:<403> but was:<200>
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```
→ With the mutation, the un-listed origin is reflected (`ACAO: *`) and the preflight returns **200** instead of 403 — the test **fails as designed**. **Reverted** to deny-by-default; the suite is green again. *(Injection-safety's teeth are shown inline by the vulnerable-vs-parameterized contrast: 1 vs 0.)*

**4 · `smoke.sh`** — `bash steps/step-18/smoke.sh` → checks the threat-model artifacts name R-001/BOLA, runs both test classes, and greps the hardening into `SecurityConfig` → **`✅ Step 18 smoke test PASSED`**.

**5 · Clean-room (§12.4)** — fresh clone, `make verify` → BUILD SUCCESS across all 9 modules. *(Full output in the commit's clean-room run.)*

---

<a id="apply"></a>

# E · 🎓 Apply

## 🚀 Go Deeper (Optional)

<details><summary>Why CORS is not a security control</summary>CORS only constrains *browsers* (they honor the preflight). curl, Postman, and other servers ignore it entirely. So a locked-down CORS policy prevents a malicious *website* from scripting your API in a victim's browser, but does nothing against a direct attacker. Your real access control is the JWT + (eventually) object-level authz. Treat CORS as one layer, never *the* layer.</details>

<details><summary>STRIDE per element vs attack trees</summary>STRIDE-per-element is *breadth* — it sweeps every box/arrow for all six categories so you don't forget a class of bug. Attack trees are *depth* — pick one attacker goal ("move funds out") and enumerate the paths, marking which are closed. Use both: STRIDE to find candidate issues, an attack tree to reason about whether a specific crown-jewel goal is reachable. (Our "steal money" tree shows every branch closed *except* BOLA.)</details>

## 💼 Interview Prep

1. **What is BOLA / IDOR, and how do you prevent it?** *Broken Object Level Authorization: an endpoint acts on an object id from the request without checking the caller owns it. Prevent it by enforcing object-level authorization on every access — derive the owner from the authenticated principal and compare, don't trust the id in the request. It's the #1 API risk because authentication alone doesn't stop it.* **(Most commonly asked.)**
2. **Authentication vs authorization — and where did our bank stop?** *Authn = who you are (valid JWT, Step 17); authz = what you may do. We have authn + function-level authz (`@PreAuthorize` admin) but not object-level authz — hence BOLA.*
3. **How do you *really* stop SQL injection?** *Parameterized queries / prepared statements (ORMs do this) so input is always data, never code. Not escaping, not blocklists. We prove it with a test that runs the same payload through parameterized (0 matches) and concatenated (all matches) paths.*
4. **STRIDE the login flow.** *S: brute force → BCrypt + lockout; T: forge token → RS256; R: deny login → audit log; I: leak hashes → never return them; D: credential stuffing → rate limit; E: self-grant role → set roles server-side.*
5. **Is CORS a security boundary?** *No — it's a browser guardrail. Non-browser clients ignore it. Deny-by-default is good hygiene but the JWT is the gate.*
6. **(Gotcha) You find a critical authz bug late in a sprint. Ship a quick fix or not?** *Record it in the risk register (severity, owner, date), ship only a complete fix; a half-enforced authz fix creates false confidence. Schedule the proper ownership-model change. — a STAR-able judgment answer.*

## 🏋️ Your Turn: Practice & Challenges

- **Quick:** add a `Permissions-Policy` header; assert it in `SecurityHardeningTest`. <details><summary>Hint</summary>Spring Security 6+ doesn't have a dedicated DSL method for it on all versions — use `.headers(h -> h.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "geolocation=()")))`.</details>
- **Quick:** make cif a resource server too (close **R-002**) — reuse Step 17's `jwk-set-uri` config and a slice test.
- 🎓 **Phase-C Capstone — STRIDE one feature end-to-end + secure it.** Take the **v1 transfer** and walk every element of its data flow with STRIDE (client→gateway→controller→service→DB→webhook), as in `security/threat-model.md §7`. Then **implement the BOLA fix (R-001)** as a stretch reference solution: add `owner_subject` to `account` (Flyway migration), set it from `jwt.getSubject()` on open, enforce ownership on `transfer`/`balance`/`entries` (non-owner → 403, admin override), and add tests (owner 200 / non-owner 403 / admin 200). Reference solution lives in `solutions/step-18/` (or the `solutions` branch). *This is the real capstone — you'll have threat-modeled a feature and closed the system's #1 risk.*

---

<a id="review"></a>

# F · 🏆 Review

## 🩺 Stuck? Troubleshooting & Fixes

- **`expected single matching bean but found 2: corsConfigurationSource, mvcHandlerMappingIntrospector`** (context fails to load, *all* demand-account tests error). **Cause:** you injected `CorsConfigurationSource` by type into `filterChain` — Spring MVC contributes a second bean of that type. **Fix:** drop the parameter; use `.cors(Customizer.withDefaults())`, which resolves the bean **by name** `corsConfigurationSource`. *(I hit this exact error building this step.)*
- **HSTS header missing in tests.** Expected — Spring only emits `Strict-Transport-Security` over a real TLS connection; plain-HTTP MockMvc/HttpClient won't show it. Don't assert it over HTTP.
- **CORS preflight returns 200 in your test.** Your allow-list isn't empty (env var set?) or you allowed `*`. Deny-by-default needs an empty origins list.
- **Reset to a known-good state:** `git checkout step-18-end`. Run `make doctor` if the toolchain looks off.

## 📚 Learn More & Glossary

- OWASP Top 10 (2021), OWASP API Security Top 10 (2023), OWASP Cheat Sheets (CORS, Injection, Headers); Microsoft/Shostack STRIDE; the SECURITY of Spring Security reference (Headers, CORS).
- **Glossary:** *STRIDE* (6 threat categories); *DFD* (data-flow diagram); *trust boundary* (where trust level changes); *BOLA/IDOR* (object-level authz failure); *CORS* (browser cross-origin policy); *parameterized query* (input bound as data); *risk register* (owned list of open risks).

## 🏆 Recap & Study Notes

**(a) Key points:** Threat modeling = "what can go wrong?" *before* an attacker asks. STRIDE-per-element over a DFD with trust boundaries finds threat *classes* systematically. Our model surfaced **BOLA (R-001, CRITICAL)** — recorded & scheduled, not faked. We shipped cheap, complete edge hardening (headers + deny-by-default CORS) with tests, and proved injection-safety with a vulnerable contrast.

**(b) Key terms:** STRIDE, DFD, trust boundary, BOLA/IDOR, CORS, parameterized query, risk register, secure-by-default.

**(c) 🧠 Test Yourself:** ① Name the six STRIDE letters. ② Why is BOLA an authorization (not authentication) bug? ③ Why isn't CORS a security control? ④ How do you actually stop SQL injection? ⑤ Why record R-001 instead of quick-fixing it? <details><summary>Answers</summary>① Spoofing/Tampering/Repudiation/Info-disclosure/DoS/Elevation. ② Authn passes (valid token); the missing check is *ownership* of the object. ③ Only browsers honor it; other clients ignore it — the JWT is the gate. ④ Parameterized queries (input as data, never code). ⑤ A half-enforced authz fix manufactures false confidence; a tracked register + complete fix is safer.</details>

**(d) 🔗 How this connects:** audits Step 17 (authz), Step 14 (signed webhooks), Step 13 (ProblemDetail), Steps 8–10 (parameterized JPA). **Next: Step 19** opens Phase D — messaging & events (Kafka) — which adds new elements and trust boundaries to *re-threat-model*.

**(e) 🏆 Résumé line:** *"Threat-modeled a banking platform with STRIDE (DFD, trust boundaries, attack trees), mapped findings to OWASP Top 10 / API Security Top 10, shipped secure-by-default hardening (CSP-class headers, deny-by-default CORS) with tests, and maintained a prioritized risk register including a tracked critical BOLA finding."*

**(f) ✅ You can now:** STRIDE a system · spot/explain BOLA · harden headers + CORS and test them · prove injection-safety · keep a risk register.

**(g) 🃏 Flashcards** (added to `docs/flashcards.md`) · 🔁 revisit BOLA when you build the authorization/ownership step.

**(h) ✍️ One-line reflection:** *What surprised you more — that your bank had a critical bug, or that the right move was to write it down rather than rush a fix?*

**(i)** 🎉 **That's Phase C done** — you can build *and defend* web APIs. Phase D makes the bank distributed. Onward.
