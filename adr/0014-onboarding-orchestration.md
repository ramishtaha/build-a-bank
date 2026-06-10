# ADR-0014: Retail onboarding as an orchestration over declarative HTTP clients (with compensation)

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 23 — Retail Services onboarding orchestration; Phase D

## Context
Onboarding a retail customer spans two services that don't share a transaction: **CIF** (create the customer)
and **demand-account** (open their account). We need a coordinated workflow that completes both or leaves the
system consistent if one fails. Step 21 built a **choreography/Saga** (event-driven, no coordinator); Step 23
deliberately builds the contrasting **orchestration** flavour.

## Decision

### 1. A thin `services/onboarding` orchestrator (no database)
A small coordinator service owns the onboarding flow end-to-end. It has **no database** — its job is to call
other services in order and decide what to do on failure. Port 8086.

### 2. Orchestration over declarative HTTP clients (`@HttpExchange`)
`OnboardingService.onboard` calls CIF then demand-account through **declarative HTTP interface clients**
(`@HttpExchange` over `RestClient` + `HttpServiceProxyFactory`, with connect/read timeouts) — the Step-15
pattern, reused. One coordinator knows the whole flow (easy to read, test, and reason about), in contrast to
choreography where the flow is implicit across event handlers.

### 3. Compensation, not rollback
Each step is a remote call that commits independently, so recovery is a **compensating action**: if opening
the account fails after the customer was created, the orchestrator calls CIF to **deactivate** the customer
(KYC → REJECTED) and surfaces an `OnboardingFailedException` (→ 502). This required a small, additive CIF
endpoint `POST /api/customers/{id}/deactivate` (the real compensation target). Like a Saga, the workflow is
**not isolated** — between steps a customer exists with no account — which we handle with the compensation.

### 4. Identity propagation
demand-account is a secured resource server (Step 17), so the orchestrator **forwards the caller's bearer
token** (the `Authorization` header) downstream. CIF has no auth yet (R-002), so no token is needed there.

### 5. Testing with in-process stubs (no Docker)
The orchestration is proven over **real HTTP** against an in-process `StubDownstream` controller (the Step-15
in-test-stub pattern): the test app serves stub `/api/customers` + `/api/accounts` on a random port, the
clients point at it, and the stub can be told to fail the account step. This deterministically exercises the
call sequence, the compensation, and token forwarding — without standing up the real services or Docker.

## Consequences
- ✅ A real cross-service workflow with a clear coordinator; the orchestration vs choreography contrast is
  concrete (Step 21 Saga vs Step 23 orchestration).
- ✅ Compensation proven: a forced account-open failure deactivates the just-created customer (verified).
- ✅ Identity propagation proven (the forwarded bearer reaches the account call).
- ✅ Fast, deterministic, Docker-free orchestration test.
- ⚠️ The orchestrator is **synchronous** — it blocks while the downstream calls run, and a crash mid-flow
  (after step 1, before compensation) could leave a half-onboarded customer. A durable, recoverable workflow
  (persisted saga state, retries) is the next maturity step (Step 52 / a workflow engine).
- ⚠️ onboarding has **no auth** itself yet (R-002) — put it behind the gateway later; it forwards, but does
  not mint, tokens (a service/client-credentials token is a later auth step).
- ⚠️ The live flow needs CIF + demand-account + auth running; the automated proof uses in-process stubs (§12.8).
- 🔁 Step 24 (Spring Batch + Phase-D capstone), Step 41 (auth server — service tokens), Step 52 (durable
  workflows / event sourcing). The gateway (Step 15) would front onboarding for external callers.
