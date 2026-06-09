// services/demand-account/src/main/java/com/buildabank/account/event/TransferCompletedEvent.java
package com.buildabank.account.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Step 20 · a <strong>domain event</strong>: "a transfer completed." Published in-process via Spring's
 * {@code ApplicationEventPublisher} the moment the money has moved (still inside the transfer transaction),
 * then handled by {@code @TransactionalEventListener}s after the transaction's outcome is known.
 *
 * <p>It carries everything a downstream consumer needs to react (notify a customer, update a read model)
 * <em>without</em> calling back into this service. {@code eventId} is a stable, unique id used as the
 * <strong>idempotency / dedupe key</strong> end-to-end (Outbox row id → Kafka message → consumer dedupe), so
 * an at-least-once pipeline yields exactly-once <em>effect</em> (Step 19 theory, made real here).
 */
public record TransferCompletedEvent(
        UUID eventId,
        UUID transactionId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        Instant occurredAt) {

    /** Factory that mints a fresh event id for a just-committed transfer. */
    public static TransferCompletedEvent of(UUID transactionId, String from, String to, BigDecimal amount) {
        return new TransferCompletedEvent(UUID.randomUUID(), transactionId, from, to, amount, Instant.now());
    }
}
