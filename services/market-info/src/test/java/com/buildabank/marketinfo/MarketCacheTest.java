// services/market-info/src/test/java/com/buildabank/marketinfo/MarketCacheTest.java
package com.buildabank.marketinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Step 22 · proves the Redis cache read model, the {@code @CachePut} refresh, and {@code @Async} on virtual
 * threads — against a REAL Redis (Testcontainers).
 *
 * <p>Each test uses a <strong>unique currency pair</strong> so cache entries never bleed between methods (the
 * context — and so the Redis cache + the {@link RateProvider} counter — is shared across the module's tests),
 * and assertions use provider call-count <strong>deltas</strong>. A cache <em>miss</em> always calls upstream
 * synchronously (deterministic), but a just-written entry becomes GET-visible only after a brief network
 * round-trip — a Redis cache is an eventually-consistent, networked store — so we {@code await} until a repeat
 * read is served from cache rather than assuming instant read-after-write.
 */
@SpringBootTest
@Import(RedisContainers.class)
class MarketCacheTest {

    @Autowired
    MarketRateService rates;

    @Autowired
    RateProvider provider;

    @Autowired
    CacheWarmer warmer;

    @Test
    void repeatReadsAreServedFromCache_notUpstream() {
        FxRate first = rates.getRate("AAA", "BBB");        // miss → calls the slow upstream, populates the cache

        // Once the entry is visible, a repeat read is served from Redis (no new upstream call) and is identical.
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            int callsBefore = provider.callCount();
            FxRate again = rates.getRate("AAA", "BBB");
            assertThat(provider.callCount()).isEqualTo(callsBefore);   // this read did NOT hit upstream
            assertThat(again).isEqualTo(first);                         // same cached value
        });

        int beforeNewPair = provider.callCount();
        rates.getRate("AAA", "CCC");                       // a different key → a miss → upstream again
        assertThat(provider.callCount()).isEqualTo(beforeNewPair + 1);
    }

    @Test
    void refreshOverwritesTheCachedValue() {
        FxRate original = rates.getRate("DDD", "EEE");      // cached
        int afterFirstRead = provider.callCount();

        FxRate refreshed = rates.refreshRate("DDD", "EEE"); // @CachePut → upstream + overwrite cache
        assertThat(provider.callCount() - afterFirstRead).isEqualTo(1);
        assertThat(refreshed).isNotEqualTo(original);        // drift → a genuinely new value
        assertThat(refreshed.asOf()).isGreaterThanOrEqualTo(original.asOf());

        // The refreshed value is now what reads return (await visibility), with no further upstream call.
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            int callsBefore = provider.callCount();
            FxRate afterRefresh = rates.getRate("DDD", "EEE");
            assertThat(provider.callCount()).isEqualTo(callsBefore);   // served from cache
            assertThat(afterRefresh).isEqualTo(refreshed);
        });
    }

    @Test
    void asyncWarmRunsOnAVirtualThread() throws Exception {
        CompletableFuture<Boolean> ranOnVirtualThread = warmer.warm("FFF", "GGG");
        assertThat(ranOnVirtualThread.get(5, TimeUnit.SECONDS)).isTrue();
    }
}
