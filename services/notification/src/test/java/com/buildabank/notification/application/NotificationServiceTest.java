// services/notification/src/test/java/com/buildabank/notification/application/NotificationServiceTest.java
package com.buildabank.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buildabank.notification.application.port.out.NotificationPublisher;
import com.buildabank.notification.application.port.out.ProcessedEventStore;
import com.buildabank.notification.domain.Notification;
import com.buildabank.notification.domain.TransferEvent;

/**
 * Step 28 · a <strong>fast, Docker-free unit test of the use case</strong> — the payoff of the hexagon
 * (Step 26): because {@link NotificationService} depends only on its outbound ports, we mock them with Mockito
 * and exercise the core logic in microseconds, with no Kafka and no Spring context. (Step 26's "Your Turn"
 * challenge, now delivered.) These tests are what PITest mutates against in the §-D mutation-coverage capstone.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    ProcessedEventStore processedEvents;

    @Mock
    NotificationPublisher publisher;

    private static TransferEvent event() {
        return new TransferEvent("evt-1", "txn-1", "ACC-1", "ACC-2", new BigDecimal("100.00"), "2026-06-10T00:00:00Z");
    }

    @Test
    void aNewEventIsAppliedAndPublishedExactlyOnce() {
        when(processedEvents.markIfNew("evt-1")).thenReturn(true);
        NotificationService service = new NotificationService(processedEvents, publisher);

        boolean applied = service.handle(event());

        assertThat(applied).as("a new event is applied").isTrue();
        ArgumentCaptor<Notification> pushed = ArgumentCaptor.forClass(Notification.class);
        verify(publisher).publish(pushed.capture());
        assertThat(pushed.getValue().transactionId()).isEqualTo("txn-1");
        assertThat(pushed.getValue().message()).contains("100.00", "ACC-1", "ACC-2");
    }

    @Test
    void aDuplicateEventIsIgnoredAndNeverPublished() {
        when(processedEvents.markIfNew("evt-1")).thenReturn(false);
        NotificationService service = new NotificationService(processedEvents, publisher);

        boolean applied = service.handle(event());

        assertThat(applied).as("a duplicate is an idempotent no-op").isFalse();
        verify(publisher, never()).publish(any());
    }
}
