// services/notification/src/main/java/com/buildabank/notification/TransferEventConsumer.java
package com.buildabank.notification;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The Kafka consumer for {@code transfer.completed} events. After the Step-25 refactor it is a thin
 * <strong>orchestration</strong> of single-responsibility collaborators it depends on through abstractions
 * (Dependency Inversion): a {@link TransferEventParser} (parsing), a {@link ProcessedEventStore} port
 * (idempotency), and the {@link SseHub} (push). The flow is one line each: parse → dedupe → notify.
 *
 * <p><strong>Idempotent consumer = exactly-once effect (Step 19/20).</strong> Kafka delivers at-least-once;
 * {@code ProcessedEventStore.markIfNew} makes a duplicate a no-op. A poison payload throws in the parser and is
 * NOT swallowed, so the container routes it to the Dead-Letter Topic (Step 21).
 */
@Component
public class TransferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);

    private final TransferEventParser parser;
    private final ProcessedEventStore processedEvents;
    private final SseHub hub;
    private final AtomicInteger received = new AtomicInteger();
    private final AtomicInteger applied = new AtomicInteger();

    public TransferEventConsumer(TransferEventParser parser, ProcessedEventStore processedEvents, SseHub hub) {
        this.parser = parser;
        this.processedEvents = processedEvents;
        this.hub = hub;
    }

    @KafkaListener(
            topics = "${bank.events.topic:transfers.completed}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onTransferCompleted(String payload) {
        received.incrementAndGet();
        TransferEvent event = parser.parse(payload);          // poison → throws → Dead-Letter Topic
        if (!processedEvents.markIfNew(event.eventId())) {
            log.info("duplicate event {} ignored (exactly-once effect)", event.eventId());
            return;                                            // duplicate → idempotent skip
        }
        Notification notification = Notification.from(event);
        applied.incrementAndGet();
        hub.publish(notification);
        log.info("notified: {}", notification.message());
    }

    /** Total messages delivered to this consumer (includes duplicates). */
    public int receivedCount() {
        return received.get();
    }

    /** Distinct events actually applied (duplicates excluded) — should equal the number of real transfers. */
    public int appliedCount() {
        return applied.get();
    }
}
