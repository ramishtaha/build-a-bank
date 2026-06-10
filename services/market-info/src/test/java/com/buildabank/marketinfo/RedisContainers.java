// services/market-info/src/test/java/com/buildabank/marketinfo/RedisContainers.java
package com.buildabank.marketinfo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real Redis for tests — backs both the Spring Cache and the ShedLock lock store.
 * {@code @ServiceConnection(name = "redis")} wires {@code spring.data.redis.*} at this container. Pinned image.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisContainers {

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    }
}
