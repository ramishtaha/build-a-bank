// services/notification/src/main/java/com/buildabank/notification/InMemoryProcessedEventStore.java
package com.buildabank.notification;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * The in-memory <strong>adapter</strong> for {@link ProcessedEventStore} — a thread-safe set of seen event ids.
 * Simple and fast, but resets on restart (so a restart could reprocess). A Redis/DB adapter (Step 21's
 * Idempotency Key) would make it durable; thanks to the port, swapping it needs no consumer change.
 */
@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markIfNew(String eventId) {
        return processed.add(eventId);   // Set.add returns true only the first time → idempotency guard
    }
}
