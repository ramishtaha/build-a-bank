// services/market-info/src/main/java/com/buildabank/marketinfo/RateProvider.java
package com.buildabank.marketinfo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Step 22 · the (simulated) <strong>expensive</strong> upstream that the cache exists to avoid hammering —
 * think a paid FX-rate API with latency and rate limits. It sleeps a little and <strong>counts its calls</strong>,
 * which is how the tests prove the cache works: two cached reads of the same pair must hit this provider only
 * once. A tiny per-call drift makes a refresh produce an observably newer rate.
 */
@Component
public class RateProvider {

    private static final Map<String, BigDecimal> BASE = Map.of(
            "USD/EUR", new BigDecimal("0.92"),
            "USD/GBP", new BigDecimal("0.79"),
            "EUR/USD", new BigDecimal("1.09"));

    private final AtomicInteger calls = new AtomicInteger();

    /** Fetch a rate from the "upstream" (slow). Each call is counted and drifts slightly from the last. */
    public FxRate fetch(String base, String quote) {
        int n = calls.incrementAndGet();
        sleepBriefly();   // simulate network/compute cost the cache will save us from
        BigDecimal baseRate = BASE.getOrDefault(base + "/" + quote, BigDecimal.ONE);
        BigDecimal rate = baseRate.add(new BigDecimal("0.0001").multiply(BigDecimal.valueOf(n)));
        return new FxRate(base, quote, rate, System.currentTimeMillis());
    }

    /** How many times the upstream was actually called (the cache should keep this low). */
    public int callCount() {
        return calls.get();
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
