# 🧳 Capsule - Step 23

**Exists now:** 13 Maven modules, `./mvnw verify` BUILD SUCCESS at `step-23-end`. New `services/onboarding` (port 8086, no DB) exposes `POST /api/onboarding`; cif now has `POST /api/customers/{id}/deactivate` (KYC → REJECTED). Tests: onboarding 4 (`OnboardingOrchestrationTest` ×2, `OnboardingControllerTest` ×2 — 201 + 400) + the cif deactivate case in `CustomerControllerTest`. `smoke.sh` PASSED; clean-room green.

**This step added:**
- `services/onboarding`: `OnboardingApplication`, `CifClient`/`AccountClient` (`@HttpExchange`, Step-15 pattern), request/response records, `HttpInterfaceClients` factory + `ClientConfig` (connect/read timeouts), `OnboardingService` orchestrator, `OnboardingFailedException` (→ 502), `OnboardingController`, `application.yml` (`services.cif.url` / `services.account.url`), root `pom.xml` registration.
- Bearer-token forwarding: the caller's `Authorization` header is relayed on the secured demand-account call.
- cif: `CustomerService.deactivate` + `POST /api/customers/{id}/deactivate` → 204 — the compensation target.
- `OnboardingOrchestrationTest`: real HTTP against in-process `StubDownstream` (no Docker); compensation proven by `deactivateCalls == [42]`.
- ADR-0014 (synchronous orchestrator; not crash-recoverable mid-flow).

**Gotchas:**
- Compensation, not rollback: each step commits in its own DB; the flow is NOT isolated (a customer briefly exists with no account).
- `RestClient` throws on 4xx/5xx — compensation must run in the catch around the account-open step.
- Response records need `@JsonIgnoreProperties(ignoreUnknown = true)`; the annotations live in `com.fasterxml.jackson.annotation` (present on Jackson 3 too).
- `@HttpExchange` parameters use `org.springframework.web.bind.annotation` annotations; a null header value is simply omitted.
- Onboarding has no auth of its own yet (R-002, gateway later); the cif deactivate test needs Docker (Testcontainers Postgres), the orchestration tests don't.

**Callback hooks:**
- Orchestration (coordinator) vs Step-21 choreography/Saga — revisited at the Phase-D capstone (Step 24) and durable workflows (Step 52).
- §12.3 mutation evidence: removing `cif.deactivate(...)` fails `accountOpenFails…:89` with `Expecting [] to contain [42]`; reverted.
- Token relay is the user-initiated pattern; a service acting on its own behalf will use client-credentials in a later auth step.

**Next step starts:** `step-23-end == step-24-start`. Green: `./mvnw verify` (13 modules), onboarding 4 tests + cif deactivate, `smoke.sh` PASSED, tagged `step-23-end`. Step 24 = Spring Batch + the Phase-D capstone (closes Phase D).
