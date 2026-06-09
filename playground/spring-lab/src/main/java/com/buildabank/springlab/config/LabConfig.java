// playground/spring-lab/src/main/java/com/buildabank/springlab/config/LabConfig.java
package com.buildabank.springlab.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A {@code @Configuration} class with a {@code @Bean} <strong>factory method</strong>, and the place we
 * turn on {@link BankProperties} binding via {@code @EnableConfigurationProperties}.
 *
 * <p>By default {@code @Configuration} is "full" mode ({@code proxyBeanMethods=true}): calling
 * {@code clock()} from another {@code @Bean} method returns the SAME singleton, because Spring intercepts
 * the call via a CGLIB proxy of this config class.
 */
@Configuration
@EnableConfigurationProperties(BankProperties.class)
public class LabConfig {

    /** A UTC clock bean — injectable anywhere we need "now" (and mockable in tests). */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
