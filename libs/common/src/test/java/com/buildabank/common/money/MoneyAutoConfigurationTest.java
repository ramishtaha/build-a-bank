// libs/common/src/test/java/com/buildabank/common/money/MoneyAutoConfigurationTest.java
package com.buildabank.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 28 · the textbook way to test a starter — {@link ApplicationContextRunner} spins up a tiny real Spring
 * context with the auto-configuration applied (exactly as Boot would discover it), but with no full app and no
 * Docker. We assert all four behaviours that make a starter correct: it configures by default, properties bind,
 * it backs off when the consumer defines their own bean, and it can be switched off.
 */
class MoneyAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MoneyAutoConfiguration.class));

    @Test
    void autoConfiguresAMoneyFormatterByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MoneyFormatter.class);
            assertThat(context.getBean(MoneyFormatter.class).currencyCode()).isEqualTo("USD");
        });
    }

    @Test
    void bindsTheCurrencyProperty() {
        runner.withPropertyValues("buildabank.money.currency-code=EUR")
                .run(context -> assertThat(context.getBean(MoneyFormatter.class).currencyCode()).isEqualTo("EUR"));
    }

    @Test
    void backsOffWhenTheConsumerDefinesItsOwn() {
        runner.withUserConfiguration(CustomFormatterConfig.class).run(context -> {
            assertThat(context).hasSingleBean(MoneyFormatter.class);  // ours did NOT also register
            assertThat(context.getBean(MoneyFormatter.class).currencyCode()).isEqualTo("JPY");
        });
    }

    @Test
    void canBeDisabledByProperty() {
        runner.withPropertyValues("buildabank.money.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MoneyFormatter.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFormatterConfig {
        @Bean
        MoneyFormatter myOwnFormatter() {
            return new MoneyFormatter("JPY");
        }
    }
}
