// services/market-info/src/main/java/com/buildabank/marketinfo/MarketInfoApplication.java
package com.buildabank.marketinfo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Step 22 · the Market Info service. Serves FX/reference rates as a read-optimized, Redis-cached read model.
 * {@code @EnableCaching} turns on {@code @Cacheable}; {@code @EnableAsync} turns on {@code @Async} (which runs
 * on virtual threads here — see {@code spring.threads.virtual.enabled}). Cluster-safe scheduling
 * ({@code @EnableScheduling} + ShedLock) is switched on in {@link SchedulingConfig}.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class MarketInfoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketInfoApplication.class, args);
    }
}
