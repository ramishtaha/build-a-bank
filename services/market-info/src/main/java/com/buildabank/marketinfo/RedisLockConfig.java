// services/market-info/src/main/java/com/buildabank/marketinfo/RedisLockConfig.java
package com.buildabank.marketinfo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;

/**
 * Step 22 · the ShedLock {@link LockProvider}, backed by the Redis we already run. ShedLock stores a lock row
 * (here, a Redis key with a TTL) so that across a <strong>cluster</strong> of market-info instances, only one
 * acquires the lock and runs a {@code @SchedulerLock}-annotated job — the others skip that tick. Declared
 * unconditionally (separate from {@link SchedulingConfig}) so tests can exercise the lock directly even with
 * the scheduler switched off.
 */
@Configuration
public class RedisLockConfig {

    @Bean
    LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }
}
