// services/cif/src/test/java/com/buildabank/cif/ContainersConfig.java
package com.buildabank.cif;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spins up a REAL PostgreSQL in a container for tests. {@code @ServiceConnection} tells Spring Boot to
 * point the application's DataSource at this container automatically — no JDBC URL/credentials in test
 * config. The image is pinned (never {@code latest}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // Testcontainers 2.0: PostgreSQLContainer is no longer generic (the 1.x self-type <SELF> was dropped).
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }
}
