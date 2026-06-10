// services/market-info/src/main/java/com/buildabank/marketinfo/SchedulingConfig.java
package com.buildabank.marketinfo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * Step 22 · turns on {@code @Scheduled} ({@link EnableScheduling}) and ShedLock's {@code @SchedulerLock} aspect
 * ({@link EnableSchedulerLock}) in production. Gated by {@code market.scheduling.enabled} (default true) so
 * tests can switch the timer off and drive the refresh / lock deterministically. {@code defaultLockAtMostFor}
 * is the safety net: if a node dies holding the lock, it's released after this long so another node can run.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
@ConditionalOnProperty(name = "market.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
