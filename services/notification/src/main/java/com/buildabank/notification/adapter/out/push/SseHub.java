// services/notification/src/main/java/com/buildabank/notification/adapter/out/push/SseHub.java
package com.buildabank.notification.adapter.out.push;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.buildabank.notification.application.port.out.NotificationPublisher;
import com.buildabank.notification.domain.Notification;

/**
 * Step 26 (hexagonal) · the outbound (driven) PUSH adapter — implements {@link NotificationPublisher} using
 * <strong>Server-Sent Events</strong>. Holds the open {@link SseEmitter}s (one per connected browser) and a
 * recent buffer; {@code publish} broadcasts to all. The application core pushes through the port and never
 * sees an {@code SseEmitter}. Thread-safe (copy-on-write emitters + guarded buffer) — Kafka listener threads
 * publish while request threads subscribe (Step 11). The web adapter uses {@link #register}/{@link #recent}
 * to serve the SSE endpoints (the SSE transport is shared between push-out and subscribe-in).
 */
@Component
public class SseHub implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);
    private static final int RECENT_LIMIT = 50;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Deque<Notification> recent = new ArrayDeque<>();

    /** Register a new SSE subscriber; auto-removes itself on completion/timeout/error. */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L);   // 0 = no timeout (stream stays open)
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    /** Outbound port: record and broadcast a notification to every connected client. */
    @Override
    public void publish(Notification notification) {
        synchronized (recent) {
            recent.addFirst(notification);
            while (recent.size() > RECENT_LIMIT) {
                recent.removeLast();
            }
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("transfer").data(notification));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);   // client gone → drop it
                log.debug("dropped a dead SSE subscriber", e);
            }
        }
    }

    /** The most recent notifications, newest first (for a just-connected client or a test). */
    public List<Notification> recent() {
        synchronized (recent) {
            return new ArrayList<>(recent);
        }
    }

    /** Number of currently-connected SSE subscribers. */
    public int subscriberCount() {
        return emitters.size();
    }
}
