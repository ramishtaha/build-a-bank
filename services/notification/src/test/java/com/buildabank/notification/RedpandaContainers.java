// services/notification/src/test/java/com/buildabank/notification/RedpandaContainers.java
package com.buildabank.notification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real Kafka-compatible broker (Redpanda) for the notification consumer tests. {@code @ServiceConnection}
 * points {@code spring.kafka.*} (and hence the {@code @KafkaListener}) at this container. Image pinned — see VERSIONS.md.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedpandaContainers {

    @Bean
    @ServiceConnection
    RedpandaContainer redpandaContainer() {
        return new RedpandaContainer(DockerImageName.parse("redpandadata/redpanda:v24.2.7"));
    }
}
