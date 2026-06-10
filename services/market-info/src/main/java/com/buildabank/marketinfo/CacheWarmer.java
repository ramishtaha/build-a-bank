// services/market-info/src/main/java/com/buildabank/marketinfo/CacheWarmer.java
package com.buildabank.marketinfo;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Step 22 · warms the rate cache <strong>off the request thread</strong> using {@code @Async}. Because
 * {@code spring.threads.virtual.enabled=true}, the async executor runs tasks on <strong>virtual threads</strong>
 * (Project Loom) — cheap to spawn by the thousands, ideal for these mostly-waiting calls (Step 11). Returns a
 * {@link CompletableFuture} the caller can join; the boolean reports whether it really ran on a virtual thread
 * (proven by a test). It calls {@link MarketRateService#getRate} on another bean so the cache proxy applies.
 */
@Component
public class CacheWarmer {

    private final MarketRateService rates;

    public CacheWarmer(MarketRateService rates) {
        this.rates = rates;
    }

    /** Asynchronously populate the cache for a pair; the future resolves to "did this run on a virtual thread?". */
    @Async
    public CompletableFuture<Boolean> warm(String base, String quote) {
        rates.getRate(base, quote);                       // populates the @Cacheable entry
        return CompletableFuture.completedFuture(Thread.currentThread().isVirtual());
    }
}
