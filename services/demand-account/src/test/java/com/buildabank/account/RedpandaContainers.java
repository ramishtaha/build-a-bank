// services/demand-account/src/test/java/com/buildabank/account/RedpandaContainers.java
package com.buildabank.account;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spins up a REAL Kafka-compatible broker (Redpanda) for tests. {@code @ServiceConnection} wires Spring's
 * {@code spring.kafka.*} (bootstrap servers) at this container automatically — so the KafkaTemplate and any
 * listener point at it with no manual config. Image pinned (never {@code latest}) — see VERSIONS.md.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedpandaContainers {

    @Bean
    @ServiceConnection
    RedpandaContainer redpandaContainer() {
        return new RedpandaContainer(DockerImageName.parse("redpandadata/redpanda:v24.2.7"));
    }
}
