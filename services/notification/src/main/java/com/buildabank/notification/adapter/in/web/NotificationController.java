// services/notification/src/main/java/com/buildabank/notification/adapter/in/web/NotificationController.java
package com.buildabank.notification.adapter.in.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.buildabank.notification.adapter.out.push.SseHub;
import com.buildabank.notification.domain.Notification;

/**
 * Step 26 (hexagonal) · the inbound (driving) WEB adapter exposing the notification stream.
 * {@code GET /api/notifications/stream} opens a live Server-Sent Events connection;
 * {@code GET /api/notifications} returns the recent buffer. It drives the SSE push adapter directly — the SSE
 * transport is shared between pushing notifications out and clients subscribing in (a deliberate, documented
 * coupling; everything that matters — domain/application purity — is enforced by ArchUnit in Step 27).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SseHub hub;

    public NotificationController(SseHub hub) {
        this.hub = hub;
    }

    /** Subscribe to the live event stream (text/event-stream). The connection stays open and pushes events. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return hub.register();
    }

    /** The most recent notifications (newest first). */
    @GetMapping
    public List<Notification> recent() {
        return hub.recent();
    }
}
