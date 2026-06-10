// services/market-info/src/main/java/com/buildabank/marketinfo/MarketController.java
package com.buildabank.marketinfo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Step 22 · the Market Info read API. {@code GET /api/market/rates/{base}/{quote}} returns the (Redis-cached)
 * FX rate. The first request for a pair is slow (upstream fetch); repeats are served from the cache until they
 * expire or the scheduled refresh updates them.
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketRateService rates;

    public MarketController(MarketRateService rates) {
        this.rates = rates;
    }

    @GetMapping("/rates/{base}/{quote}")
    public FxRate rate(@PathVariable String base, @PathVariable String quote) {
        return rates.getRate(base.toUpperCase(), quote.toUpperCase());
    }
}
