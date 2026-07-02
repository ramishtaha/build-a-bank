# 🧳 Capsule - Step 15

**Exists now:**
- 8 Maven modules, full `./mvnw verify` green; new reactor member: `gateway/`.
- Services: gateway :8080 (single front door), cif :8081, demand-account :8082 (Postgres 5433, Testcontainers).
- Tests: gateway **1** (`GatewayRoutingTest`), demand-account **27** (incl. 2 `CifClientTest`).

**This step added:**
- `gateway/` — Spring Cloud Gateway **Server WebMVC** (servlet variant, ADR-0007) on Boot 4.0.6 / Spring Cloud 2025.1.1: routes `/cif/**`→cif and `/bank/**`→demand-account, `StripPrefix=1`, `AddResponseHeader=X-Gateway, build-a-bank`; actuator exposes `health,info,gateway`.
- `CifClient` in demand-account — `@HttpExchange` interface built by `HttpServiceProxyFactory` over `RestClient` (`JdkClientHttpRequestFactory`, connect+read timeouts) + `CifCustomer` DTO, `CifClientFactory`, `CifClientConfig`.
- `steps/step-15/{requests.http,smoke.sh}`, ADR-0007.

**Gotchas:**
- Config prefix is `spring.cloud.gateway.server.webmvc.routes` (Spring Cloud 2025+); the old `spring.cloud.gateway.mvc.*` is deprecated and silently won't bind (404 everything).
- Starter trap: `spring-cloud-starter-gateway-server-webmvc` (servlet), NOT `spring-cloud-starter-gateway` (reactive, drags WebFlux/Netty).
- Property spelling differs by module: gateway route reads `services.cif.uri`; demand-account client reads `services.cif.url`.
- Routing-test stub must start inside `@DynamicPropertySource` (before context); read timeout surfaces as `ResourceAccessException`.

**Callback hooks:**
- The gateway is where edge auth (Steps 16-17), rate limiting (Step 51), and k8s ingress/discovery (Step 34+) will slot in.
- Timeouts are only the resilience baseline; circuit breakers/retries/bulkheads deferred to Resilience4j (Step 37).
- `CifClient` sync HTTP is the "before" picture for async Kafka events (Step 20).

**Next step starts:**
- `step-15-end` == `step-16-start`. Green: full verify (8 modules), §12.3 StripPrefix mutation caught + reverted, `smoke.sh` PASSED, clean-room clone verified.
