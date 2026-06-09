// services/demand-account/src/test/java/com/buildabank/account/RedisContainers.java
package com.buildabank.account;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spins up a REAL Redis for tests. {@code @ServiceConnection(name = "redis")} tells Spring Boot to point
 * {@code spring.data.redis.*} at this container automatically (the Redis connection-details factory matches on
 * the "redis" name). Image pinned (never {@code latest}) — see VERSIONS.md.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisContainers {

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    }
}
