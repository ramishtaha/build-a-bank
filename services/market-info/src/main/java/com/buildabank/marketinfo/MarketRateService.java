// services/market-info/src/main/java/com/buildabank/marketinfo/MarketRateService.java
package com.buildabank.marketinfo;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Step 22 · the FX-rate read model. {@link #getRate} is {@code @Cacheable} on Redis (cache name
 * {@code fxRates}): the first read for a pair calls the slow {@link RateProvider}; subsequent reads are served
 * from Redis until the entry expires or is refreshed. This is a small <strong>CQRS read model</strong> — a
 * read-optimized, eventually-consistent view that's decoupled from (and cheaper than) the authoritative source.
 *
 * <p>{@link #refreshRate} is {@code @CachePut}: it always calls the provider and <em>writes</em> the result
 * into the cache (used by the scheduled refresh to keep the read model fresh). NOTE: these methods are invoked
 * from <em>other</em> beans (the controller, the refresh job) so the Spring cache proxy actually applies —
 * a {@code this.}-call would bypass it (the self-invocation pitfall, Step 7).
 */
@Service
public class MarketRateService {

    static final String CACHE = "fxRates";

    private final RateProvider provider;

    public MarketRateService(RateProvider provider) {
        this.provider = provider;
    }

    /** Read a rate — cached. Cache key is "BASE/QUOTE". */
    @Cacheable(cacheNames = CACHE, key = "#base + '/' + #quote")
    public FxRate getRate(String base, String quote) {
        return provider.fetch(base, quote);
    }

    /** Force-refresh a rate from upstream and overwrite the cache entry (keeps the read model fresh). */
    @CachePut(cacheNames = CACHE, key = "#base + '/' + #quote")
    public FxRate refreshRate(String base, String quote) {
        return provider.fetch(base, quote);
    }
}
