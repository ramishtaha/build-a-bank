// services/demand-account/src/main/java/com/buildabank/account/client/CifClientConfig.java
package com.buildabank.account.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link CifClient} bean, built from config (base URL + timeouts). In a full deployment the
 * base URL points at the CIF service (or, behind the gateway, at the gateway); the timeouts come from config
 * so ops can tune them per environment.
 */
@Configuration
public class CifClientConfig {

    @Bean
    public CifClient cifClient(
            @Value("${services.cif.url:http://localhost:8081}") String baseUrl,
            @Value("${services.cif.connect-timeout-ms:2000}") long connectMs,
            @Value("${services.cif.read-timeout-ms:2000}") long readMs) {
        return CifClientFactory.create(baseUrl, Duration.ofMillis(connectMs), Duration.ofMillis(readMs));
    }
}
