# 🧳 Capsule - Step 7

**Exists now:** (as of `step-07-end`) Maven reactor BUILD SUCCESS with **40 tests**: hello (2) + java-basics (22) + playground/spring-lab (16). spring-lab is a web app: `GET /api/accounts` + `GET /api/accounts/{id}` (200 JSON / 404) on :8080 via `spring-boot:run`; `smoke.sh` boots it on :8082. Verification tier: 🟠 Standard.

**This step added:**
- deps: `spring-boot-starter-web`, `org.aspectj:aspectjweaver`, `spring-boot-webmvc-test` (test scope) — all BOM-managed, no versions pinned
- `aop` package: `@Audited` (RUNTIME marker) + `AuditCounter` (CopyOnWriteArrayList) + `AuditAspect` (`@Around`, logs method signature only, never args)
- `account` package (Phase-A capstone slice): `Account` record (BigDecimal balance) → `InMemoryAccountStore` (2 demo accounts) → `AccountService` (3 `@Audited` methods; `summaryFor` deliberately self-invokes `findById`) → `AccountController`
- `ProxyInspectorRunner` — prints `AccountService$$SpringCGLIB$$0`, isAopProxy/isCglibProxy both true
- tests: `AccountControllerTest` (3, MockMvc) + `AuditAspectSelfInvocationTest` (3, counter proof) — spring-lab 10 → 16

**Gotchas:**
- Boot 4 removed `spring-boot-starter-aop` (no GA) → depend on `org.aspectj:aspectjweaver` directly; Boot's `AopAutoConfiguration` enables @AspectJ proxying
- Boot 4 moved `@AutoConfigureMockMvc` to `org.springframework.boot.webmvc.test.autoconfigure` (artifact `spring-boot-webmvc-test`, NOT transitive from starter-test)
- aspect needs `@Component` in addition to `@Aspect`; `@Around` must call `proceed()` and re-throw; `@Audited` needs RUNTIME retention
- self-invocation: `this.findById()` inside `summaryFor` bypasses the proxy — counter proves 1, not 2; CGLIB can't advise final/private methods
- ports: app on :8080, smoke.sh on :8082; console once rendered the ▶/✔ log glyphs as mojibake

**Callback hooks:**
- self-invocation pitfall → Step 12 (`@Transactional` on the ledger) and Steps 16–17 (`@PreAuthorize` method security)
- CGLIB subclass proxies are Boot's default (`proxyTargetClass=true` since Boot 2.0) — proven at runtime via `AopUtils`
- `AuditCounter` thread-safety (`CopyOnWriteArrayList` as shared mutable state) → deep dive in Step 11

**Next step starts:** `step-07-end == step-08-start` — **Phase A complete**. Green: `./mvnw verify` (40 tests), capstone slice 200/404 over real HTTP, `AUDIT ▶/✔` log lines, `steps/step-07/smoke.sh` PASSED. Step 8 = the CIF service + Spring Data JPA.
