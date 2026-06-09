// playground/spring-lab/src/main/java/com/buildabank/springlab/autoconfig/GreetingAutoConfiguration.java
package com.buildabank.springlab.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.buildabank.springlab.config.BankProperties;

/**
 * A real (tiny) <strong>auto-configuration</strong> — exactly how Spring Boot configures beans based on
 * what is on the classpath and in your properties. It is discovered because its fully-qualified name is
 * listed in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 *
 * <ul>
 *   <li>{@code @ConditionalOnProperty(... matchIfMissing=true)} — on unless {@code bank.greeting.enabled=false};</li>
 *   <li>{@code @ConditionalOnMissingBean} — backs off if YOU already defined a {@code GreetingService}
 *       (this "sensible default you can override" behavior is the heart of Boot auto-configuration).</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "bank.greeting.enabled", havingValue = "true", matchIfMissing = true)
public class GreetingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GreetingService greetingService(BankProperties properties) {
        return new GreetingService(properties.name());
    }
}
