// playground/spring-lab/src/test/java/com/buildabank/springlab/rates/ConditionalBeansTest.java
package com.buildabank.springlab.rates;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Tests {@code @ConditionalOnProperty} WITHOUT booting the whole app, using {@link ApplicationContextRunner}
 * — a lightweight, fast way to assert which beans the container would (or would not) create for a given config.
 */
class ConditionalBeansTest {

    @Configuration
    @Import({FixedRateProvider.class, MarketRateProvider.class})
    static class RatesConfig {
    }

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(RatesConfig.class);

    @Test
    void fixedProviderIsTheDefault() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(FixedRateProvider.class)
                .doesNotHaveBean(MarketRateProvider.class));
    }

    @Test
    void marketProviderOnlyWhenConfigured() {
        runner.withPropertyValues("bank.rates.source=market")
                .run(context -> assertThat(context)
                        .hasSingleBean(MarketRateProvider.class)
                        .doesNotHaveBean(FixedRateProvider.class));
    }
}
