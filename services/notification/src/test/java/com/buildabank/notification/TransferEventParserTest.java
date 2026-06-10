// services/notification/src/test/java/com/buildabank/notification/TransferEventParserTest.java
package com.buildabank.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.buildabank.notification.adapter.in.messaging.TransferEventParser;
import com.buildabank.notification.domain.TransferEvent;

import tools.jackson.databind.json.JsonMapper;

/**
 * Step 25 · a payoff of the refactor: parsing is now testable on its own — no Kafka, no Spring context.
 * A valid payload becomes a {@link TransferEvent}; a poison payload throws (so the consumer can route it to
 * the Dead-Letter Topic).
 */
class TransferEventParserTest {

    private final TransferEventParser parser = new TransferEventParser(JsonMapper.builder().build());

    @Test
    void parsesAValidPayloadIntoADomainEvent() {
        TransferEvent event = parser.parse("""
                {"eventId":"E1","transactionId":"T1","from":"ACC-A","to":"ACC-B","amount":40.00,"occurredAt":"2026-06-10T00:00:00Z"}
                """);

        assertThat(event.eventId()).isEqualTo("E1");
        assertThat(event.transactionId()).isEqualTo("T1");
        assertThat(event.fromAccount()).isEqualTo("ACC-A");
        assertThat(event.toAccount()).isEqualTo("ACC-B");
        assertThat(event.amount()).isEqualByComparingTo("40.00");
        assertThat(event.occurredAt()).isEqualTo("2026-06-10T00:00:00Z");
    }

    @Test
    void poisonPayloadThrows_soTheConsumerCanRouteItToTheDlt() {
        assertThatThrownBy(() -> parser.parse("<<<not-valid-json>>>"))
                .isInstanceOf(Exception.class);
    }
}
