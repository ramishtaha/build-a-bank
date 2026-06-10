// services/notification/src/main/java/com/buildabank/notification/application/NotificationService.java
package com.buildabank.notification.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.buildabank.notification.application.port.in.NotifyOnTransfer;
import com.buildabank.notification.application.port.out.NotificationPublisher;
import com.buildabank.notification.application.port.out.ProcessedEventStore;
import com.buildabank.notification.domain.Notification;
import com.buildabank.notification.domain.TransferEvent;

/**
 * Step 26 (hexagonal) · the APPLICATION use case — implements the inbound port {@link NotifyOnTransfer} and
 * orchestrates the domain through outbound ports only. It depends inward (domain) and sideways onto its own
 * ports — never on an adapter or a framework's transport types. Idempotent: dedupe via
 * {@link ProcessedEventStore}, build the {@link Notification}, push via {@link NotificationPublisher}.
 */
@Service
public class NotificationService implements NotifyOnTransfer {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ProcessedEventStore processedEvents;
    private final NotificationPublisher publisher;

    public NotificationService(ProcessedEventStore processedEvents, NotificationPublisher publisher) {
        this.processedEvents = processedEvents;
        this.publisher = publisher;
    }

    @Override
    public boolean handle(TransferEvent event) {
        if (!processedEvents.markIfNew(event.eventId())) {
            log.info("duplicate event {} ignored (exactly-once effect)", event.eventId());
            return false;   // duplicate → idempotent no-op
        }
        Notification notification = Notification.from(event);
        publisher.publish(notification);
        log.info("notified: {}", notification.message());
        return true;
    }
}
