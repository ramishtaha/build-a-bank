# 🧳 Capsule - Step 5

**Exists now:** 3 modules — `services/hello`, `playground/java-basics`, `playground/spring-lab` (new; non-web, no endpoints/ports — pure JVM). 28 tests green repo-wide (+6 in spring-lab). Java 25 + Spring Boot 4.0.6 pinned by the parent POM.

**This step added:**
- `playground/spring-lab` module (ADR-0003): non-web Spring Boot `CommandLineRunner` app, `spring-boot-starter` (core, no web)
- `RateProvider` strategy + conditional beans: `FixedRateProvider` (rate 0.0325, `matchIfMissing=true`) / `MarketRateProvider` (0.0475, `havingValue="market"`)
- `InterestService` (constructor DI, `BigDecimal` HALF_EVEN → 325.00 fixed / 475.00 market on 10000.00)
- `LabConfig` `@Bean Clock` (full-mode `@Configuration`); `LifecycleBean` + `TimingBeanPostProcessor` (lifecycle order 1→4 + `@PreDestroy` last)
- Prototype-scoped `AuditEntry`; `LabRunner` (prints wired provider, SpEL 3.25%, interest, scopes); `application.yml` (`bank.rates.source=fixed`, `bank.rates.fixed=0.0325`, `bank.name=Build-a-Bank`)
- 3 test classes / 6 tests (`ConditionalBeansTest` via `ApplicationContextRunner` + 2 `@SpringBootTest` contexts); `steps/step-05/smoke.sh`

**Gotchas:**
- `@Value("${bank.name}")` has no default — startup fails with `Could not resolve placeholder 'bank.name'` until `application.yml` exists (first runnable state is after sub-step 10)
- `java -jar target/spring-lab-0.1.0-SNAPSHOT.jar` needs `./mvnw -pl playground/spring-lab -am package` first; `spring-boot:run`/`compile` never build the jar
- The `annual rate (via SpEL)` log line reads `bank.rates.fixed` and prints 3.25% even in market mode — by design, not a bug
- `jakarta.annotation.*` not `javax.*`; prototypes get no `@PreDestroy`; `smoke.sh` needs Git Bash/WSL on Windows; `requests.http` waived for this step (non-web, nothing to curl)

**Callback hooks:**
- `@ConditionalOn…` machinery + `@ConfigurationProperties` (stretch goal preview) return in Step 6 auto-config
- `BeanPostProcessor` is the exact hook Step 7's AOP proxy-weaving uses
- Singleton statelessness/thread-safety (race forced-then-fixed) lands in Step 11

**Next step starts:** tag `step-05-end` == step-6-start. Green: `./mvnw -pl playground/spring-lab -am verify` (6 tests), repo-wide 28 tests BUILD SUCCESS, `smoke.sh` PASSED (🟠 Standard tier).
