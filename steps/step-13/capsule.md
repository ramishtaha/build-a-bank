# 🧳 Capsule - Step 13

**Exists now:**
- Full repo: 7 modules, `./mvnw verify` green; `services/demand-account` at **13 tests** (TransferControllerTest 5 · DemandAccountIntegrationTest 2 · ConcurrentTransferTest 2 · OptimisticLockTest 1 · TransactionPropagationTest 1 · TransferServiceTest 2) on real Postgres 17 via Testcontainers.
- demand-account live on **8082** (compose Postgres on **5433**): `POST /api/accounts`, `POST /api/transfers`, `GET /api/accounts/{id}`, `/v3/api-docs` (OpenAPI 3.1), `/swagger-ui.html`.

**This step added:**
- `GlobalExceptionHandler` (RFC 9457 ProblemDetail): overdraw → 422 `application/problem+json` {type,title:"Insufficient funds",status,detail,instance}; validation → 400 + `errors.amount` map. Step-12 `ApiExceptionHandler` deleted.
- springdoc-openapi **3.0.3** (pinned) + `OpenApiConfig` → live `/v3/api-docs` + Swagger UI 200.
- `RequestIdFilter` (OncePerRequestFilter, `X-Request-Id`) + `TimingInterceptor`/`WebConfig` (`X-Timing-Enabled`, timing log line).
- Tests 11 → 13; §12.3 mutation (422→200) fails as expected and was reverted; `steps/step-13/smoke.sh` PASSED.

**Gotchas:**
- springdoc must be **3.0.x** on Boot 4 (2.8.x targets Boot 3); it is NOT Boot-managed — version pinned in `VERSIONS.md`.
- Interceptors need `WebMvcConfigurer.addInterceptors` registration (`@Component` alone never runs); filters auto-register as beans.
- Response headers in `postHandle` are unreliable (body may be committed) — set headers in `preHandle`, log in `afterCompletion`; per-request state in request attributes, never instance fields.
- Live runs need `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/demand_account` (compose maps 5433, not 5432).

**Callback hooks:**
- Step 14 (API design/versioning/webhooks) builds on the ProblemDetail + OpenAPI contract.
- Steps 16-17 (Spring Security) slot into the servlet **filter chain** introduced here.
- Step 36 wires `X-Request-Id` into distributed tracing.

**Next step starts:** tag `step-13-end` == `step-14-start`; green: full `./mvnw verify` (7 modules), 13 demand-account tests, live Swagger UI + problem+json verified, smoke.sh PASSED, clean-room re-clone BUILD SUCCESS.
