// services/market-info/src/test/java/com/buildabank/marketinfo/ShedLockTest.java
package com.buildabank.marketinfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

/**
 * Step 22 · proves the ShedLock {@link LockProvider} (Redis) gives the cluster guarantee that
 * {@code @SchedulerLock} relies on: while one holder owns a named lock, a second acquire is refused — so only
 * one node would run a scheduled job — and after release the lock is acquirable again. Verified against a REAL
 * Redis (Testcontainers); this is what stops every clustered instance from running the refresh on each tick.
 */
@SpringBootTest
@Import(RedisContainers.class)
class ShedLockTest {

    @Autowired
    LockProvider lockProvider;

    @Test
    void aHeldLockBlocksAnotherAcquire_andIsReacquirableAfterRelease() {
        Optional<SimpleLock> first = lockProvider.lock(lockConfig("refreshFxRates-test"));
        assertThat(first).as("first acquire succeeds").isPresent();

        Optional<SimpleLock> second = lockProvider.lock(lockConfig("refreshFxRates-test"));
        assertThat(second).as("second acquire is refused while held (the cluster guard)").isEmpty();

        first.get().unlock();

        Optional<SimpleLock> third = lockProvider.lock(lockConfig("refreshFxRates-test"));
        assertThat(third).as("re-acquirable after release").isPresent();
        third.get().unlock();
    }

    /** lockAtLeastFor = 0 so unlock releases immediately (otherwise the lock is held until at-least elapses). */
    private static LockConfiguration lockConfig(String name) {
        return new LockConfiguration(Instant.now(), name, Duration.ofSeconds(30), Duration.ZERO);
    }
}
