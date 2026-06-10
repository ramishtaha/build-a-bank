// services/notification/src/main/java/com/buildabank/notification/application/port/out/ProcessedEventStore.java
package com.buildabank.notification.application.port.out;

/**
 * Step 26 (hexagonal) · OUTBOUND (driven) port for idempotency (introduced as a port in Step 25). The use case
 * depends on this abstraction; a driven adapter implements it (in-memory today, Redis tomorrow — Step 21's
 * pattern) with no change to the core.
 */
public interface ProcessedEventStore {

    /**
     * Atomically record that an event id has been seen.
     *
     * @return {@code true} if this id is NEW (process it); {@code false} if it's a duplicate (skip it)
     */
    boolean markIfNew(String eventId);
}
