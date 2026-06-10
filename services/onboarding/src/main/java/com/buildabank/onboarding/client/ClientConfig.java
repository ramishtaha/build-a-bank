// services/onboarding/src/main/java/com/buildabank/onboarding/client/ClientConfig.java
package com.buildabank.onboarding.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the downstream HTTP clients from config. Base URLs default to the services' local ports; behind a
 * gateway they'd point at the gateway. Timeouts are config-tunable per environment.
 */
@Configuration
public class ClientConfig {

    @Bean
    CifClient cifClient(
            @Value("${services.cif.url:http://localhost:8081}") String baseUrl,
            @Value("${services.cif.connect-timeout-ms:2000}") long connectMs,
            @Value("${services.cif.read-timeout-ms:5000}") long readMs) {
        return HttpInterfaceClients.create(CifClient.class, baseUrl,
                Duration.ofMillis(connectMs), Duration.ofMillis(readMs));
    }

    @Bean
    AccountClient accountClient(
            @Value("${services.account.url:http://localhost:8082}") String baseUrl,
            @Value("${services.account.connect-timeout-ms:2000}") long connectMs,
            @Value("${services.account.read-timeout-ms:5000}") long readMs) {
        return HttpInterfaceClients.create(AccountClient.class, baseUrl,
                Duration.ofMillis(connectMs), Duration.ofMillis(readMs));
    }
}
