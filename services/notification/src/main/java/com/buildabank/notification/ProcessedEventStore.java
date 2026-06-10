// services/notification/src/main/java/com/buildabank/notification/ProcessedEventStore.java
package com.buildabank.notification;

/**
 * Step 25 · a <strong>port</strong> (Dependency Inversion Principle): the consumer's idempotency depends on
 * this abstraction, not on a concrete data structure. Today the only adapter is in-memory
 * ({@link InMemoryProcessedEventStore}); a durable adapter (Redis/DB, like Step 21) can replace it without
 * touching the consumer — DIP + the Open/Closed Principle.
 */
public interface ProcessedEventStore {

    /**
     * Atomically record that an event id has been seen.
     *
     * @return {@code true} if this id is NEW (process it); {@code false} if it's a duplicate (skip it)
     */
    boolean markIfNew(String eventId);
}
