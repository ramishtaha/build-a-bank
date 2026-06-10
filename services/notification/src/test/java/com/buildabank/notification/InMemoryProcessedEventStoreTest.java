// services/notification/src/test/java/com/buildabank/notification/InMemoryProcessedEventStoreTest.java
package com.buildabank.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.buildabank.notification.adapter.out.persistence.InMemoryProcessedEventStore;
import com.buildabank.notification.application.port.out.ProcessedEventStore;

/**
 * Step 25 · the idempotency port's in-memory adapter, tested in isolation (another refactor payoff): the first
 * sighting of an id is new; a repeat is a duplicate. This is the guard behind exactly-once effect.
 */
class InMemoryProcessedEventStoreTest {

    private final ProcessedEventStore store = new InMemoryProcessedEventStore();

    @Test
    void firstSightingIsNew_repeatIsADuplicate() {
        assertThat(store.markIfNew("E1")).isTrue();    // new → process
        assertThat(store.markIfNew("E1")).isFalse();   // duplicate → skip
        assertThat(store.markIfNew("E2")).isTrue();    // a different id is new again
    }
}
