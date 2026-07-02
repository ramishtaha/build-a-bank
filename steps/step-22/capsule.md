# 🧳 Capsule - Step 22

**Exists now:** 12 modules green (`./mvnw verify` BUILD SUCCESS). New `services/market-info` (port 8085, no DB, no auth yet — R-002): `GET /api/market/rates/{base}/{quote}`. Redis (Step 21) is now also a cache + ShedLock lock store. market-info tests: **5** (MarketCacheTest 3, ShedLockTest 1, MarketControllerTest 1), all on real Redis except the standalone-MockMvc controller test.

**This step added:**
- `@Cacheable("fxRates")` read model on Redis (`MarketRateService.getRate`) + `@CachePut refreshRate`; both share SpEL key `#base + '/' + #quote`; TTL 60s (`spring.cache.redis.time-to-live`).
- `CacheWarmer.warm` `@Async` on **virtual threads** (`AsyncConfig`: `SimpleAsyncTaskExecutor.setVirtualThreads(true)` — scoped to `@Async`, not app-wide).
- `RateRefreshJob` `@Scheduled` + `@SchedulerLock("refreshFxRates")` — ShedLock **6.10.0**, Redis `LockProvider` (`RedisLockConfig`), gated by `market.scheduling.enabled`.
- Tests use Testcontainers `redis:7.4-alpine` via `@ServiceConnection(name="redis")`; §12.3 mutation (removed `@Cacheable` → `ConditionTimeout … but was: 21`, reverted); smoke.sh PASSED; ADR-0013.

**Gotchas:**
- Networked-cache write-visibility: don't assert read-after-write instantly — the cache test `await`s until a repeat read is served from cache.
- `@WebMvcTest` fails to load with caching/Redis enabled — the thin controller is tested with **standalone MockMvc** (no Spring context).
- Self-invocation: `@Cacheable`/`@SchedulerLock` apply only through the proxy — cross-bean calls, public methods (Step 7).
- Scheduling is OFF in tests (`market.scheduling.enabled=false` in test properties); tests drive `RateRefreshJob.refresh()`/the LockProvider directly.

**Callback hooks:**
- The cache is the **read side of CQRS** — full CQRS/event sourcing at Step 52; cache metrics at Step 36.
- market-info has **no auth** (like cif/notification — R-002); it goes behind the gateway later.
- Ports: hello 8080 · cif 8081 · demand-account 8082 · auth 8083 · notification 8084 · **market-info 8085**.

**Next step starts:** `step-22-end == step-23-start` (Step 23: retail onboarding orchestration). Green at handoff: `./mvnw verify` (12 modules), market-info 5/5 on real Redis, `bash steps/step-22/smoke.sh` PASSED, tag `step-22-end`.
