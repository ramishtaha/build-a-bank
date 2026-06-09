// services/notification/src/test/java/com/buildabank/notification/NotificationControllerTest.java
package com.buildabank.notification;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Step 20 · web-layer slice for the notification API. Confirms {@code GET /api/notifications} returns the
 * recent buffer and {@code GET /api/notifications/stream} opens an async Server-Sent Events response. The
 * {@link SseHub} is mocked (no Kafka in the slice).
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SseHub hub;

    @Test
    void recentReturnsTheBufferedNotifications() throws Exception {
        given(hub.recent()).willReturn(List.of(new Notification(
                "evt-1", "txn-1", "ACC-A", "ACC-B", new BigDecimal("40.00"),
                "2026-06-10T00:00:00Z", "Transfer of 40.00 from ACC-A to ACC-B completed.")));

        mvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$[0].message").value("Transfer of 40.00 from ACC-A to ACC-B completed."));
    }

    @Test
    void streamOpensAnSseConnection() throws Exception {
        given(hub.register()).willReturn(new SseEmitter());

        mvc.perform(get("/api/notifications/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())                                   // SSE = async response
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
