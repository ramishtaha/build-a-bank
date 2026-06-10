// services/notification/src/main/java/com/buildabank/notification/application/port/in/NotifyOnTransfer.java
package com.buildabank.notification.application.port.in;

import com.buildabank.notification.domain.TransferEvent;

/**
 * Step 26 (hexagonal) · INBOUND (driving) port — the use case the application offers to the outside world:
 * "given a transfer event, notify (idempotently)." Driving adapters (the Kafka listener) depend on this
 * interface, not on the implementation.
 */
public interface NotifyOnTransfer {

    /**
     * Handle a transfer event.
     *
     * @return {@code true} if it was newly applied (a notification was pushed); {@code false} if it was a
     *         duplicate (idempotent no-op)
     */
    boolean handle(TransferEvent event);
}
