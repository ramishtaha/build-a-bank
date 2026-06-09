// playground/spring-lab/src/main/java/com/buildabank/springlab/rates/RateProvider.java
package com.buildabank.springlab.rates;

import java.math.BigDecimal;

/**
 * A strategy for obtaining the annual interest rate. Two implementations exist
 * ({@code FixedRateProvider}, {@code MarketRateProvider}); exactly one is wired at runtime,
 * chosen by configuration via {@code @ConditionalOnProperty}. This is the Strategy pattern +
 * Dependency Inversion: {@code InterestService} depends on this interface, not a concrete class.
 */
public interface RateProvider {

    String name();

    BigDecimal annualRate();
}
