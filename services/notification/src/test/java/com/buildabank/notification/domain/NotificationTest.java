// services/notification/src/test/java/com/buildabank/notification/domain/NotificationTest.java
package com.buildabank.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Step 28 · example-based unit test of the domain factory {@link Notification#from}. Pairs with the jqwik
 * property test ({@link NotificationPropertyTest}): this pins the <em>exact</em> message wording for one case;
 * the property checks the <em>invariants</em> hold for thousands of generated cases.
 */
class NotificationTest {

    @Test
    void fromMapsEveryFieldAndComposesTheMessage() {
        TransferEvent event = new TransferEvent(
                "evt-9", "txn-9", "ACC-A", "ACC-B", new BigDecimal("250.00"), "2026-06-10T12:00:00Z");

        Notification n = Notification.from(event);

        assertThat(n.eventId()).isEqualTo("evt-9");
        assertThat(n.transactionId()).isEqualTo("txn-9");
        assertThat(n.fromAccount()).isEqualTo("ACC-A");
        assertThat(n.toAccount()).isEqualTo("ACC-B");
        assertThat(n.amount()).isEqualByComparingTo("250.00");
        assertThat(n.occurredAt()).isEqualTo("2026-06-10T12:00:00Z");
        assertThat(n.message()).isEqualTo("Transfer of 250.00 from ACC-A to ACC-B completed.");
    }
}
