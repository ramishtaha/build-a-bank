# ADR-0003: Phase-A learning code lives in `playground/*` modules

- **Status:** Accepted
- **Date:** 2026-06-09
- **Deciders:** Build-a-Bank (autonomous senior default)

## Context
Phase A (Steps 1–7) teaches fundamentals — Java, the JVM, Spring core, Boot internals, AOP — before the real banking
microservices begin at Step 8 (CIF). This learning code must build and be tested (the chain invariant + §12), but it
is **not** part of the production service set in the repo structure (§13: `services/*`, `libs/common`, `gateway`, …).
Mixing throwaway learning classes into `services/` would muddy the architecture learners study later.

## Decision
Phase-A learning code lives under a top-level **`playground/`** area, one module per theme:
- `playground/java-basics` — Step 2 (Java language), extended in Step 3 (networking) and Step 4 (JVM lab).
- `playground/spring-lab` — Steps 5–7 (Spring Core, Boot internals, AOP), growing into the Phase-A capstone vertical slice.

`services/hello` (Step 1) remains the minimal toolchain-proof app. Each `playground` module is a normal Maven module
listed in the root aggregator, inheriting the Spring Boot parent for managed dependency/plugin versions. They use
**banking-flavoured examples** (Money/Account/Transaction) so the fundamentals seed the real domain.

## Consequences
- ✅ Clean separation: `services/` stays reserved for the real microservices; learning code is clearly labelled.
- ✅ Everything still builds under one `./mvnw verify`; the chain invariant holds.
- ✅ Examples are domain-relevant, so Step 8+ reuses the mental models (BigDecimal money, UTC time, repositories).
- 🔁 At Step 8 we start fresh in `services/cif`; `playground/*` may be kept as reference or pruned (documented then).
