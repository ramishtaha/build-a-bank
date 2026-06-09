// playground/spring-lab/src/main/java/com/buildabank/springlab/rates/MarketRateProvider.java
package com.buildabank.springlab.rates;

import java.math.BigDecimal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pretends to fetch a live market rate. Registered as a bean ONLY when {@code bank.rates.source=market}.
 * Swapping providers requires zero code change in {@code InterestService} — just configuration.
 */
@Component
@ConditionalOnProperty(name = "bank.rates.source", havingValue = "market")
public class MarketRateProvider implements RateProvider {

    @Override
    public String name() {
        return "market";
    }

    @Override
    public BigDecimal annualRate() {
        return new BigDecimal("0.0475"); // a pretend live rate
    }
}
