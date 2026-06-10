// services/notification/src/main/java/com/buildabank/notification/adapter/in/messaging/TransferEventConsumer.java
package com.buildabank.notification.adapter.in.messaging;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.buildabank.notification.application.port.in.NotifyOnTransfer;
import com.buildabank.notification.domain.TransferEvent;

/**
 * Step 26 (hexagonal) · the inbound (driving) MESSAGING adapter: a Kafka listener that translates the wire
 * payload (via {@link TransferEventParser}) and drives the inbound port {@link NotifyOnTransfer}. It holds no
 * business logic — parsing is the parser's job, dedupe/notify is the use case's. A poison payload throws in the
 * parser and is NOT swallowed, so the container routes it to the Dead-Letter Topic (Step 21).
 *
 * <p>The received/applied counters are an observability seam (used by tests/metrics) on the adapter edge.
 */
@Component
public class TransferEventConsumer {

    private final TransferEventParser parser;
    private final NotifyOnTransfer notifyOnTransfer;
    private final AtomicInteger received = new AtomicInteger();
    private final AtomicInteger applied = new AtomicInteger();

    public TransferEventConsumer(TransferEventParser parser, NotifyOnTransfer notifyOnTransfer) {
        this.parser = parser;
        this.notifyOnTransfer = notifyOnTransfer;
    }

    @KafkaListener(
            topics = "${bank.events.topic:transfers.completed}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onTransferCompleted(String payload) {
        received.incrementAndGet();
        TransferEvent event = parser.parse(payload);   // poison → throws → Dead-Letter Topic
        if (notifyOnTransfer.handle(event)) {
            applied.incrementAndGet();
        }
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
