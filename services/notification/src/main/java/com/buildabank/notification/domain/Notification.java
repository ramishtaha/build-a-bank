// services/notification/src/main/java/com/buildabank/notification/domain/Notification.java
package com.buildabank.notification.domain;

import java.math.BigDecimal;

/**
 * Step 26 (hexagonal) · DOMAIN value object — a customer-facing notification derived from a
 * {@link TransferEvent}. Pure domain (no framework/transport). The {@link #from} factory keeps the
 * message-wording in the core, derived from a domain event rather than a JSON payload.
 */
public record Notification(
        String eventId,
        String transactionId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String occurredAt,
        String message) {

    /** Build a notification from a domain {@link TransferEvent}. */
    public static Notification from(TransferEvent event) {
        String message = "Transfer of " + event.amount()
                + " from " + event.fromAccount() + " to " + event.toAccount() + " completed.";
        return new Notification(event.eventId(), event.transactionId(), event.fromAccount(),
                event.toAccount(), event.amount(), event.occurredAt(), message);
    }
}
