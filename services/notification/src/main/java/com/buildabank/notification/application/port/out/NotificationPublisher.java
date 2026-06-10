// services/notification/src/main/java/com/buildabank/notification/application/port/out/NotificationPublisher.java
package com.buildabank.notification.application.port.out;

import com.buildabank.notification.domain.Notification;

/**
 * Step 26 (hexagonal) · OUTBOUND (driven) port for pushing a {@link Notification} to clients. The use case
 * depends on this abstraction; the SSE adapter implements it. Swapping the push transport (WebSocket, a
 * webhook, email) is a new adapter — no core change.
 */
public interface NotificationPublisher {

    void publish(Notification notification);
}
