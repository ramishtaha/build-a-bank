// services/market-info/src/main/java/com/buildabank/marketinfo/RateRefreshJob.java
package com.buildabank.marketinfo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Step 22 · periodically refreshes the FX-rate read model. {@code @Scheduled} runs it on a timer;
 * {@code @SchedulerLock} ensures that in a <strong>cluster</strong> only ONE instance runs each tick (the
 * others find the lock held and skip) — without it, every node would hammer the upstream provider on every
 * tick. {@code lockAtMostFor} bounds a crashed holder; {@code lockAtLeastFor} prevents a too-fast re-run.
 * Calls {@link MarketRateService#refreshRate} on another bean so the {@code @CachePut} proxy applies.
 */
@Component
public class RateRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(RateRefreshJob.class);

    /** The pairs the read model keeps warm. */
    static final List<String[]> TRACKED = List.of(
            new String[]{"USD", "EUR"},
            new String[]{"USD", "GBP"},
            new String[]{"EUR", "USD"});

    private final MarketRateService rates;

    public RateRefreshJob(MarketRateService rates) {
        this.rates = rates;
    }

    @Scheduled(fixedRateString = "${market.refresh-rate-ms:60000}")
    @SchedulerLock(name = "refreshFxRates", lockAtMostFor = "PT1M", lockAtLeastFor = "PT1S")
    public void refresh() {
        for (String[] pair : TRACKED) {
            rates.refreshRate(pair[0], pair[1]);
        }
        log.info("refreshed {} FX rate(s) into the cache", TRACKED.size());
    }
}
