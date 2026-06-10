# ADR-0019: Testing & quality mastery — PITest mutation (Phase-E capstone), jqwik, a custom Spring Boot starter, and code-quality gates

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 28 — Testing mastery + your own Spring Boot starter; **closes Phase E** (incl. the Phase-E capstone)

## Context
Step 28 is the Phase-E finale. The curriculum row is "**Testing mastery + build your own Spring Boot starter**"
(test slices, MockMvc/@MockitoBean; **TDD, PITest mutation, jqwik property-based**; turn `libs/common` into a
real **auto-configured starter**), and the **Phase-E capstone** is "hexagonal (Step 26) + ArchUnit (Step 27) +
raise **mutation-test** coverage on its core to a target you justify." The §12 Definition of Done has also
listed code-quality gates (Spotless/Checkstyle/ErrorProne) as a pass-criterion since Step 1, but none existed
yet. This step delivers all of it, on JDK 25 / Spring Boot 4 — where tooling support is the live risk (§6 caveat).

## Decision

### 1. PITest mutation testing on the notification hexagon core = the Phase-E capstone
The hexagon (Step 26) made the core unit-testable; here we add fast, Docker-free tests
(`NotificationServiceTest` with Mockito, `NotificationTest`, `NotificationPropertyTest`) and run **PITest**
against the core (`Notification` + `NotificationService`). Result: **5 mutations, 5 killed — 100%** (negated
guard, removed `publish()` call, both boolean-return flips, null-return on the factory). **Justified target:**
the core is tiny and pure, so we hold it to a **90% threshold** (build fails below) and actually hit 100%.
Scoped narrowly (two classes) and excludes the Testcontainers integration tests so mutation analysis stays fast
and Docker-free. Lives in an off-by-default `-Pmutation` profile.

### 2. PITest 1.25.4 — NOT 1.19.1 (a real JDK-25 gotcha)
PITest **1.19.1** (what the Maven Central search API reported as "latest") fails on JDK 25 with
`Unsupported class file major version 69` — its bundled ASM is too old. The author confirmed (issue #1439)
support through Java 26, and **GitHub releases** show the true latest is **1.25.4** (2026-06-09), which reads
v69 bytecode. Lesson: the search API lagged; verify the *real* latest at the source. Pinned 1.25.4 +
pitest-junit5-plugin 1.2.2.

### 3. jqwik 1.9.3 for property-based testing
`NotificationPropertyTest` states an *invariant* (`Notification.from` preserves identifiers and the message
names both parties + the amount) and jqwik generates 1000 randomized cases (with shrinking). Complements the
example-based `NotificationTest`. Runs as its own JUnit-Platform engine alongside Jupiter.

### 4. `libs/common` is now a real auto-configured Spring Boot starter (`common-spring-boot-starter`)
Provides a `MoneyFormatter` (deterministic, locale-free, `BigDecimal` + HALF_EVEN) via an `@AutoConfiguration`
discovered through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, bound
from `buildabank.money.*` (`@ConfigurationProperties`), polite (`@ConditionalOnProperty(matchIfMissing=true)` +
`@ConditionalOnMissingBean`). Tested the canonical way with **`ApplicationContextRunner`** (default-on,
property-binds, backs-off, can-disable) and **really consumed** by `hello` (the lightest, Docker-free service):
its `@SpringBootTest` injects the auto-configured bean — proof Boot discovered it via the imports file. One
combined module for teaching clarity (production splits `-autoconfigure` + `-starter`; noted in the lesson).

### 5. MockMvcTester (Boot 4) — honoring the Step-1 forward-reference
The Step-1 test pointed forward to Boot 4's `MockMvcTester` "in Step 28." Delivered as a `@WebMvcTest` slice on
`hello` using the AssertJ-fluent tester (`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` —
Boot 4 modularized the slice into `spring-boot-webmvc-test`).

### 6. Code-quality gates: lean Spotless + Checkstyle, always-on; Error Prone/NullAway verified and profile-gated
- **Spotless 3.6.0** — `removeUnusedImports` + trailing-whitespace + EOF-newline, with **`lineEndings=PRESERVE`**.
  We deliberately do NOT apply a full reformatter (google-java-format) — it would reflow the course's hand-laid
  code — and PRESERVE avoids a 232-file phantom CRLF→LF diff (observed and reverted). A full formatter is a
  one-line change when a team agrees.
- **Checkstyle 13.5.0** — a **lean** ruleset (`config/checkstyle/checkstyle.xml`) of real bug-or-smell checks
  (unused/redundant imports, empty statement, one-statement-per-line, equals/hashCode, upper-ell, modifier
  order, default-comes-last, fall-through, string `==`). NOT a 200-rule Google/Sun set — retrofitting that onto
  a real codebase floods the gate and gets it switched off. **0 violations** across all modules; tightenable.
  Both bind to `verify`, so `./mvnw verify` fails on a violation.
- **Error Prone 2.49.0 + NullAway 0.13.6** — these are javac plugins, historically JDK-lagging. **Verified
  empirically: they DO work on JDK 25** (compiled `libs/common`; a planted `@Nullable` deref was flagged
  `[NullAway] dereferenced expression s is @Nullable`). Kept in an **off-by-default `-Perrorprone` profile** at
  `:WARN` (a gentle introduction; flip to `:ERROR` for a hard gate) with the full `--add-exports/--add-opens`
  javac incantation a learner needs. Spotless + Checkstyle remain the always-on gates.

### 7. §12.3 proof — the mutation gate is meaningful
Removed the `verify(publisher).publish(...)` assertion → PITest's "removed call to publish" mutant **survived**
→ score 80% < 90% → **BUILD FAILURE** (`Mutation score of 80 is below threshold of 90`). Reverted → 100% green.
A normal green build would not have caught that gap — which is the whole point of mutation testing.

## Consequences
- ✅ Phase-E capstone met: hexagonal (26) + ArchUnit (27) + **100% mutation coverage** on the core (28).
- ✅ The bank now has real testing depth (mutation, property-based, slices, the Boot-4 tester) and always-on
  formatting/style gates; the §12 DoD's "code-quality gates" is finally satisfied.
- ✅ `common-spring-boot-starter` is reusable by any service with one dependency line.
- ✅ Honest, verified tool status on bleeding-edge JDK 25 (PITest 1.25.4; Error Prone/NullAway work).
- ⚙️ Mutation + Error Prone are off-by-default profiles (slow / opt-in) — run with `-Pmutation` / `-Perrorprone`.
- ⚠️ Quality rulesets are intentionally lean (a starting point to tighten); the starter is one combined module
  (not the production `-autoconfigure`/`-starter` split); PITest scope is the notification core (extend later).
- 🔁 **End of Phase E.** Next: Phase F (Steps 29–32, React/TypeScript frontend).
