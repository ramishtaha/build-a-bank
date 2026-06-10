// services/notification/src/main/java/com/buildabank/notification/adapter/out/persistence/InMemoryProcessedEventStore.java
package com.buildabank.notification.adapter.out.persistence;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.buildabank.notification.application.port.out.ProcessedEventStore;

/**
 * Step 26 (hexagonal) · the outbound (driven) PERSISTENCE adapter for {@link ProcessedEventStore} — a
 * thread-safe set of seen event ids. Simple and fast, but resets on restart; a Redis/DB adapter (Step 21's
 * Idempotency Key) would make it durable, swappable behind the port with no core change.
 */
@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markIfNew(String eventId) {
        return processed.add(eventId);   // Set.add returns true only the first time → idempotency guard
    }
}
