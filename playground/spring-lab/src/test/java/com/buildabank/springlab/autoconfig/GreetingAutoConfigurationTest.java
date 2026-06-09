// playground/spring-lab/src/test/java/com/buildabank/springlab/autoconfig/GreetingAutoConfigurationTest.java
package com.buildabank.springlab.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.buildabank.springlab.config.BankProperties;

/**
 * Tests the custom auto-configuration the way Spring Boot tests its own: with {@link ApplicationContextRunner}
 * and {@code AutoConfigurations.of(...)}. Verifies the conditional ON/OFF behavior and the back-off.
 */
class GreetingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GreetingAutoConfiguration.class))
            .withBean(BankProperties.class, () -> new BankProperties("Build-a-Bank", null));

    @Test
    void registersGreetingServiceByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GreetingService.class);
            assertThat(context.getBean(GreetingService.class).greet("Ada"))
                    .isEqualTo("Welcome to Build-a-Bank, Ada!");
        });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("bank.greeting.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(GreetingService.class));
    }

    @Test
    void backsOffWhenUserDefinesOwnBean() {
        runner.withBean(GreetingService.class, () -> new GreetingService("Custom Bank"))
                .run(context -> assertThat(context.getBean(GreetingService.class).greet("x"))
                        .startsWith("Welcome to Custom Bank"));
    }
}
