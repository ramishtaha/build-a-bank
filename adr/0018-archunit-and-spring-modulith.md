# ADR-0018: Enforce architecture as tests — ArchUnit (the notification hexagon) + Spring Modulith (demand-account modules)

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 27 — Enforcing architecture / fitness functions; Phase E

## Context
Step 26 restructured `notification` into a hexagon (domain/application/adapter) and ADR-0017 noted the boundaries
were **conventional** — a discipline, not a guarantee. Conventions erode: the next `@Component` on a domain
record, or the next `import` from the application into an adapter, compiles fine and silently rots the design.
Step 27 makes the boundaries **executable** — *architecture fitness functions* that fail the build on a violation.
Two complementary tools, applied where each fits best.

## Decision

### 1. ArchUnit enforces the hexagon's dependency rule on `notification`
A test (`HexagonalArchitectureTest`, `@AnalyzeClasses(packages = "com.buildabank.notification")`, production only)
encodes the rules ADR-0017 wrote in prose, now as build-failing assertions over the **compiled bytecode**:
- **`hexagonal_layering`** — a `layeredArchitecture` with three rings (Domain / Application / Adapter). The Adapter
  is **one** ring, so the documented web→push SSE coupling (intra-adapter) is allowed while any
  adapter→core-inward violation fails. Application may only be accessed by Adapter; Domain only by Application+Adapter.
- **`domain_is_framework_free`** — domain classes depend on no Spring/Kafka/Jackson and no outward (application/adapter) package.
- **`application_does_not_depend_on_adapters`** and **`application_is_transport_agnostic`** — the use case sees ports + domain, never Kafka/Jackson/web.

Why ArchUnit here: the hexagon's rules are **bespoke** (which imports each ring may have, plus one allowed
exception) — exactly what ArchUnit's fluent rules express. It reads bytecode, so an *unused* `import` is invisible;
only a real reference (annotation, field, call) trips a rule — which is the correct semantics.

### 2. Spring Modulith verifies module boundaries on `demand-account` (the richest service)
A test (`ModularityTest`) derives the module model from the package structure —
`ApplicationModules.of(DemandAccountApplication.class)` finds **9 modules** (batch, client, domain, event, outbox,
payment, service, web, webhook) — and `verify()` checks the two rules that keep a modular monolith from rotting:
**no cyclic dependencies** between modules, and **no access to another module's `internal` packages**. The current
graph is a DAG (web→{domain,payment,service,webhook}, service→{domain,event,outbox}, outbox→event, batch→domain,
payment→domain), so it passes. The same test generates **living documentation** via `Documenter` (a C4 component
diagram + per-module PlantUML + a per-module "canvas") into `target/spring-modulith-docs`.

Why Modulith here: demand-account is a multi-feature service whose modules are **derived** (not hand-listed); cycle
detection across 9 modules is exactly what Modulith automates, and the generated docs can't drift from the code.

### 3. Spring Modulith is a TEST-scope tool here, not a runtime dependency
We add `spring-modulith-starter-test` + `spring-modulith-docs` at **test** scope only. `ApplicationModules.verify()`
is static bytecode analysis (ArchUnit under the hood) — it starts **no** Spring context and needs **no** Docker.
We deliberately do **not** add `spring-modulith-starter-core` to main: we want the verification + docs value
without changing demand-account's runtime autoconfiguration (module events, observability) this step. (Revisit if
the Phase-E capstone wants runtime module features.)

### 4. The §12.3 proof is an injected architecture violation
The fitness functions are only worth anything if they actually fail. We proved each: annotating the domain
`Notification` with `@Component` → ArchUnit `domain_is_framework_free` fails; adding an `event → outbox` reference
(outbox already depends on event) → Modulith reports `Cycle detected: event -> outbox`. Both reverted; green again.

## Consequences
- ✅ The hexagon (Step 26) and demand-account's module graph are now **enforced** — a regression fails the build, not review.
- ✅ Two tools, each in its sweet spot: ArchUnit for bespoke layer rules on one hexagon; Modulith for derived-module cycle detection + docs on a rich service.
- ✅ Living architecture docs (`target/spring-modulith-docs`) regenerate from code on every test run.
- ⚙️ ArchUnit reads bytecode — rules catch real references, not stray imports (correct, occasionally surprising).
- ⚠️ Modulith stays test-scoped; no runtime module features yet. Only `notification` has ArchUnit hexagon rules; other services are guarded only by Modulith cycle checks if/when added.
- 🔁 Step 28 (code-quality gates — Spotless/Checkstyle, and verify Error Prone/NullAway on JDK 25), then the Phase-E capstone (hexagonal + ArchUnit + mutation testing/PITest).
