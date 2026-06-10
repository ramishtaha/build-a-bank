// services/notification/src/main/java/com/buildabank/notification/domain/TransferEvent.java
package com.buildabank.notification.domain;

import java.math.BigDecimal;

/**
 * Step 26 (hexagonal) · DOMAIN value object — a parsed {@code transfer.completed} fact. The hexagon's core has
 * <strong>no framework, transport, or persistence imports</strong> (no Spring, no Kafka, no Jackson): it's
 * plain Java the rest of the system points <em>inward</em> at. {@code eventId} is the idempotency key.
 */
public record TransferEvent(
        String eventId,
        String transactionId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String occurredAt) {
}
