// services/notification/src/main/java/com/buildabank/notification/TransferEvent.java
package com.buildabank.notification;

import java.math.BigDecimal;

/**
 * The parsed {@code transfer.completed} event — a plain domain type with <strong>no transport coupling</strong>
 * (no JSON, no Kafka). Extracting this (Step 25, SOLID/SRP) lets the consumer work with a meaningful object
 * instead of reaching into a {@code JsonNode}, and lets parsing be tested on its own.
 */
public record TransferEvent(
        String eventId,
        String transactionId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String occurredAt) {
}
