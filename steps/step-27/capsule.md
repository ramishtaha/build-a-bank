# 🧳 Capsule - Step 27

**Exists now:** 13-module Maven repo, full `./mvnw verify` BUILD SUCCESS (Docker only for the existing integration tests). notification = Step-26 hexagon, 11 tests incl. `HexagonalArchitectureTest` (4 ArchUnit rules, no Docker). demand-account = 9 derived modules (batch, client, domain, event, outbox, payment, service, web, webhook), 47 tests incl. `ModularityTest` (3 tests, no Docker). Pinned: `archunit.version=1.4.2`, `spring-modulith.version=2.0.6` (BOM imported in parent).

**This step added:**
- `HexagonalArchitectureTest` (notification): `hexagonal_layering` (Domain/Application/Adapter, Adapter as ONE ring → web→push SSE allowed), `domain_is_framework_free`, `application_does_not_depend_on_adapters`, `application_is_transport_agnostic`
- `ModularityTest` (demand-account): `ApplicationModules.of(DemandAccountApplication.class)` → `verify()` + model print + `Documenter` → `components.puml` + 9 module canvases in `target/spring-modulith-docs/`
- pom wiring: parent `spring-modulith.version` property + `spring-modulith-bom` import; notification `archunit-junit5` (test); demand-account `spring-modulith-starter-test` + `spring-modulith-docs` (test)
- `adr/0018-archunit-and-spring-modulith.md` + `steps/step-27/smoke.sh` + 6 flashcards in `docs/flashcards.md`

**Gotchas:**
- ArchUnit reads bytecode — an unused `import` is erased by the compiler and invisible; only a real reference (annotation/field/parameter/call) trips a rule
- Wrong `@AnalyzeClasses` package (or module not compiled) → `noClasses()` rules pass vacuously ("No classes were imported")
- Modulith is TEST scope only (ADR-0018 §3): no `spring-modulith-starter-core` in main, demand-account's runtime autoconfiguration untouched
- Only notification has ArchUnit hexagon rules; other services are guarded only by Modulith checks if/when those tests are added

**Callback hooks:**
- demand-account module DAG: web→{domain,payment,service,webhook}, service→{domain,event,outbox}, outbox→event, payment→domain, batch→domain; client isolated
- Adapter modelled as ONE layer so the documented web→push SSE coupling (ADR-0017) passes while any adapter→core-inward dependency fails
- §12.3 proofs: `@Component` on domain `Notification` → ArchUnit `domain_is_framework_free` red; `event→outbox` reference → `Cycle detected: event -> outbox`; both reverted to green

**Next step starts:** tag `step-27-end == step-28-start`. Green: full `./mvnw verify` (13 modules), both architecture suites (no Docker), `bash steps/step-27/smoke.sh`. Step 28 = testing & quality mastery; the Phase-E capstone reuses hexagonal + ArchUnit and adds mutation testing (PITest).
