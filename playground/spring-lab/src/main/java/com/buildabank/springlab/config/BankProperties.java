// playground/spring-lab/src/main/java/com/buildabank/springlab/config/BankProperties.java
package com.buildabank.springlab.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration bound from {@code bank.*} properties — the grown-up alternative to scattered
 * {@code @Value} strings. Spring Boot binds {@code bank.name}, {@code bank.rates.source}, and
 * {@code bank.rates.fixed} into this immutable record via <strong>constructor binding</strong>.
 *
 * <p>Registered with {@code @EnableConfigurationProperties(BankProperties.class)} (see {@code LabConfig}).
 * Benefits over {@code @Value}: one typed object, IDE auto-completion, relaxed binding, and validation hooks.
 */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(String name, Rates rates) {

    public record Rates(String source, BigDecimal fixed) {
    }
}
