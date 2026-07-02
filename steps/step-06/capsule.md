# 🧳 Capsule — Step 6
**Exists now:**
- `playground/spring-lab` (non-web Boot app): 10 tests green — 6 from Step 5 + `BankPropertiesTest` (1) + `GreetingAutoConfigurationTest` (3). Full reactor `./mvnw verify` = 34 tests, BUILD SUCCESS.
- `services/hello` on `:8080`: Actuator exposure widened to `health,info,conditions,beans,configprops,env,mappings` (+ `health show-details: always`).
- Spring Boot 4.0.6; auto-config discovery via `.imports` only (no `spring.factories` anywhere).
**This step added:**
- `BankProperties` record (`@ConfigurationProperties(prefix="bank")`, constructor binding, nested `Rates` with `BigDecimal fixed`) + `@EnableConfigurationProperties(BankProperties.class)` on `LabConfig`.
- `GreetingService` — plain class, deliberately NOT `@Component` (enters the context only via the auto-config).
- `GreetingAutoConfiguration` — `@AutoConfiguration`, class-level `@ConditionalOnProperty(bank.greeting.enabled, matchIfMissing=true)`, `@Bean @ConditionalOnMissingBean greetingService(BankProperties)`.
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` listing it (one FQN per line).
- `LabRunner` refactored off `@Value`/SpEL onto `BankProperties` + `GreetingService`; widened `services/hello/src/main/resources/application.yml`.
- Learner tooling: `steps/step-06/requests.http` (Actuator calls) and `steps/step-06/smoke.sh`.
**Gotchas:**
- `.imports` filename/path must be exact — a typo is a silent no-op: tests still pass (`AutoConfigurations.of` bypasses discovery) but the real app dies with `UnsatisfiedDependencyException`.
- `--bank.greeting.enabled=false` makes the app FAIL to start (`LabRunner` hard-depends on `GreetingService`) — intentional demo; `ObjectProvider<GreetingService>` is the soft alternative.
- Actuator's 141 positive / 82 negative counts are machine/Boot-patch-specific; the JSON is keyed by context id (`.contexts."hello-service"`).
- `bank.*` config (`name: Build-a-Bank`, `rates.fixed: 0.0325`) has lived in the lab's `application.yml` since Step 5 — Step 6 adds no new config there.
**Callback hooks:**
- `GreetingAutoConfiguration` + the `.imports` file = the prototype of the real `libs/common` starter shipped in Step 28.
- The Actuator endpoints opened here are learning-only; they get hardened in Phase H (Step 39+).
- `@ConditionalOnMissingBean` works because auto-configs run AFTER user beans — the recurring interview hook.
**Next step starts:**
- `step-06-end == step-07-start`. Green: `./mvnw verify` (34 tests), 10 spring-lab tests, `bash steps/step-06/smoke.sh` PASSED. Step 7 = AOP & the proxy model.
