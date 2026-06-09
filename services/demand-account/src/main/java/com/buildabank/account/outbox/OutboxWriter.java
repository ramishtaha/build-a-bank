// services/demand-account/src/main/java/com/buildabank/account/outbox/OutboxWriter.java
package com.buildabank.account.outbox;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.buildabank.account.event.TransferCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serializes a {@link TransferCompletedEvent} to JSON and persists it as an {@link OutboxEvent}. Called from
 * inside the transfer transaction, so the row commits atomically with the ledger change (the Outbox pattern).
 *
 * <p>We build a plain {@link Map} and stringify {@code occurredAt}/ids rather than serialize the record
 * directly — this keeps a stable, language-neutral JSON shape for any consumer and sidesteps Java-8-time
 * Jackson modules. We own a {@code com.fasterxml} mapper because Spring Boot 4 defaults the web stack to
 * Jackson 3, so a Jackson-2 {@code ObjectMapper} bean isn't auto-created (same choice as {@code WebhookPublisher}).
 */
@Component
public class OutboxWriter {

    static final String AGGREGATE_TYPE = "transfer";
    static final String EVENT_TYPE = "transfer.completed";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutboxWriter(OutboxEventRepository repository) {
        this.repository = repository;
    }

    /** Persist the event to the outbox (within the caller's transaction). The row id IS the event id. */
    public OutboxEvent write(TransferCompletedEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", event.eventId().toString());
        body.put("transactionId", event.transactionId().toString());
        body.put("from", event.fromAccount());
        body.put("to", event.toAccount());
        body.put("amount", event.amount());
        body.put("occurredAt", event.occurredAt().toString());   // ISO-8601
        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize outbox payload", e);
        }
        return repository.save(new OutboxEvent(
                event.eventId(), AGGREGATE_TYPE, EVENT_TYPE, payload, event.occurredAt()));
    }
}
