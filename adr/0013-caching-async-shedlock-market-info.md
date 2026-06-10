# ADR-0013: A Market Info read model — Redis caching (CQRS-style), @Async on virtual threads, ShedLock scheduling

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 22 — Caching & async + Market Info + clustered scheduling; Phase D

## Context
The bank needs read-heavy reference data (FX/market rates) served fast, refreshed periodically, and correct
when run as **multiple instances**. Three Phase-D capabilities converge here: caching a read model, doing work
asynchronously, and running a scheduled job safely in a cluster. We already added **Redis** in Step 21
(idempotency); Step 22 reuses it as a cache and a lock store.

## Decision

### 1. A dedicated `services/market-info` service (no database)
Market/reference data is its own bounded context, so it gets its own small service. It has **no database** —
its state is the Redis cache plus an in-memory rate table — keeping the focus on the caching/async/scheduling
patterns rather than persistence. Port 8085.

### 2. Spring Cache on Redis as a CQRS-style read model
`MarketRateService.getRate` is `@Cacheable("fxRates")` backed by Redis (`spring.cache.type=redis`). The cached
rates are a **read-optimized, eventually-consistent read model**, decoupled from and far cheaper than the
authoritative upstream (the `RateProvider`, which simulates a slow/limited paid API and **counts its calls** so
tests can prove a cache hit). `refreshRate` is `@CachePut` (always fetch + overwrite) to keep the model fresh.
Cache methods are invoked **cross-bean** (controller, refresh job) so the proxy actually applies — a
`this.`-call would bypass it (self-invocation pitfall, Step 7). Entries TTL out after 60s (the model is
deliberately eventually consistent).

> Why "CQRS-style," not full CQRS: this is the *read* side — a separate, optimized read model. Full CQRS with a
> command side + event-sourced projections is Step 52; here we establish the read-model habit cheaply.

### 3. `@Async` on virtual threads for off-request work
`CacheWarmer.warm` is `@Async`, returning a `CompletableFuture`. With `spring.threads.virtual.enabled=true`,
the async executor (and the web server) run on **virtual threads** (Loom) — cheap for these mostly-waiting
calls (Step 11). A test asserts the work really ran on a virtual thread.

### 4. ShedLock for cluster-safe `@Scheduled`
A naive `@Scheduled` refresh would run on **every** instance each tick, hammering the upstream. **ShedLock**
(`@SchedulerLock`, Redis lock provider) ensures only one instance runs each tick; the rest find the lock held
and skip. `lockAtMostFor` bounds a crashed holder; `lockAtLeastFor` prevents a too-fast re-run.

- **Pinned** `net.javacrumbs.shedlock:shedlock-spring` + `shedlock-provider-redis-spring` **6.10.0** (not
  Boot-managed; explicit in the module POM — VERSIONS.md). The 6.x line works on Spring 7 / Boot 4 / JDK 25.
- **Scheduling is gated** (`market.scheduling.enabled`, default true) and switched **off in tests** (a
  `src/test/resources/application.properties` that merges over the main YAML), so the timer never fires
  unpredictably; tests drive the refresh and the `LockProvider` directly for deterministic assertions.

## Consequences
- ✅ Reads are fast and cheap (cache hit proven: the upstream is called once for two reads).
- ✅ The scheduled refresh is **cluster-safe** (ShedLock lock held → second acquire refused, proven on real Redis).
- ✅ Off-request work runs on virtual threads (proven).
- ✅ Reuses the Step-21 Redis — one datastore, three jobs (idempotency, cache, lock store).
- ⚠️ Cache values use Spring's default **JDK serialization** (so `FxRate` is `Serializable`); a JSON/typed
  serializer is a later refinement if cross-language cache sharing is needed.
- ⚠️ The read model is **eventually consistent** (TTL + periodic refresh) — fine for indicative rates, not for
  anything requiring strong consistency.
- ⚠️ market-info has **no auth** yet (like cif/notification, R-002) — reference data is low-sensitivity, but
  it should sit behind the gateway/security later.
- 🔁 Step 24 (batch — scheduled EOD jobs also want ShedLock), Step 36 (cache metrics/observability), Step 52
  (full CQRS + event sourcing), Step 29 (the React app consumes these rates).
