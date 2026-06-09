// playground/spring-lab/src/main/java/com/buildabank/springlab/rates/FixedRateProvider.java
package com.buildabank.springlab.rates;

import java.math.BigDecimal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Provides a fixed, configured rate. Registered as a bean ONLY when {@code bank.rates.source=fixed}
 * (or the property is absent, thanks to {@code matchIfMissing=true}) — a <strong>conditional bean</strong>.
 * The rate is injected from configuration via {@code @Value} with a sensible default.
 */
@Component
@ConditionalOnProperty(name = "bank.rates.source", havingValue = "fixed", matchIfMissing = true)
public class FixedRateProvider implements RateProvider {

    private final BigDecimal rate;

    public FixedRateProvider(@Value("${bank.rates.fixed:0.0325}") BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public String name() {
        return "fixed";
    }

    @Override
    public BigDecimal annualRate() {
        return rate;
    }
}
