# 🧳 Capsule — Step 28

**Exists now:** 14 Maven modules, full-repo `./mvnw verify` green (~3 min) with quality gates on every module. Notification hexagon core has fast no-Docker tests: `NotificationServiceTest` (Mockito, 2) + `NotificationTest` (1) + jqwik `NotificationPropertyTest` (1 property × 1000 cases). PITest `-Pmutation` on the core (`Notification` + `NotificationService`): 5/5 mutants killed = 100%, line 17/17. `libs/common` = `common-spring-boot-starter` (`MoneyFormatter`), consumed by `hello`; `MoneyAutoConfigurationTest` (ApplicationContextRunner, 4) + `MoneyFormatterTest` (3) + `HelloMockMvcTesterTest` slice. Checkstyle: 0 violations × 14 modules. No new HTTP endpoints/ports this step.

**This step added:**
- PITest 1.25.4 `-Pmutation` profile in `services/notification/pom.xml` (threshold 90, scored 100%)
- jqwik 1.9.3 property test (1000 generated cases)
- `libs/common` as a real starter: `@AutoConfiguration` + `AutoConfiguration.imports` + `@ConditionalOnMissingBean`, consumed by `hello`
- Boot-4 `MockMvcTester` slice test (`@WebMvcTest`)
- Gates in the parent pom bound to `verify`: Spotless 3.6.0 (`lineEndings=PRESERVE`) + Checkstyle 13.5.0 (lean); opt-in `-Perrorprone` (Error Prone 2.49.0 + NullAway 0.13.6, `:WARN`)
- ADR-0019 (testing/quality mastery + custom starter)

**Gotchas:**
- PITest 1.19.1 fails on JDK 25 (`Unsupported class file major version 69`) — use 1.25.4+; the Maven Central *search API* reported 1.19.1 as latest (stale — check GitHub releases)
- Spotless default line-ending handling rewrote CRLF→LF on 232 files — `lineEndings=PRESERVE` required
- PITest `targetClasses` scoped to the two core classes only (whole package → junk survivors from record `equals`/`hashCode`); Testcontainers tests excluded (a context boot per mutant is unusable)
- Boot 4: `@WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure` (dep `spring-boot-webmvc-test`); `TestRestTemplate` removed

**Callback hooks:**
- §12.3 proof pattern: delete `verify(publisher).publish(...)` → mutant SURVIVED → 80% < 90% threshold → BUILD FAILURE; revert → 100%
- Starter discovery = `AutoConfiguration.imports` (Boot 2: `spring.factories`); `@ConditionalOnMissingBean` backs off to the consumer's bean
- Error Prone/NullAway verified working on JDK 25 (opt-in `-Perrorprone`, `:WARN`)

**Next step starts:** `step-28-end == step-29-start` (🎓 Phase E complete). Green: full `verify` (14 modules, gates on), `smoke.sh` PASSED, clean-room fresh-clone. Step 29 begins the React/TS frontend.
