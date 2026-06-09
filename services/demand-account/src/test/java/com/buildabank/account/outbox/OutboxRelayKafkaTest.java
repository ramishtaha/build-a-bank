// services/demand-account/src/test/java/com/buildabank/account/outbox/OutboxRelayKafkaTest.java
package com.buildabank.account.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.redpanda.RedpandaContainer;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.RedpandaContainers;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.LedgerEntryRepository;
import com.buildabank.account.service.TransferService;

/**
 * Step 20 · proves the <strong>Outbox relay</strong> publishes pending rows to a REAL Kafka broker (Redpanda,
 * Testcontainers) and marks them published, with at-least-once semantics (a second run publishes nothing).
 * A plain Kafka consumer reads the topic to confirm the message actually landed — hard-to-fake evidence.
 */
@SpringBootTest
@Import({ContainersConfig.class, RedpandaContainers.class})
class OutboxRelayKafkaTest {

    private static final String TOPIC = "transfers.completed";

    @Autowired
    TransferService transfers;

    @Autowired
    OutboxRelay relay;

    @Autowired
    OutboxEventRepository outbox;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @Autowired
    RedpandaContainer redpanda;

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        ledger.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void relayPublishesPendingOutboxRowsToKafka_marksPublished_andIsAtLeastOnce() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("100.00"));
        transfers.openAccount("ACC-B", "USD", BigDecimal.ZERO);
        UUID txId = transfers.transfer("ACC-A", "ACC-B", new BigDecimal("40.00"), "rent");
        assertThat(outbox.countByPublishedFalse()).isEqualTo(1);

        // Drain the outbox → publish to Kafka.
        int published = relay.publishPending();
        assertThat(published).isEqualTo(1);
        assertThat(outbox.countByPublishedFalse()).isZero();          // the row is now marked published

        // Consume from the topic to PROVE the event really landed on Kafka.
        try (Consumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.key()).isNotBlank();                    // keyed by event id
            assertThat(record.value())
                    .contains(txId.toString()).contains("ACC-A").contains("ACC-B").contains("40");
        }

        // Already-published rows aren't re-sent: a second relay run publishes nothing.
        assertThat(relay.publishPending()).isZero();
    }

    private Consumer<String, String> testConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                redpanda.getBootstrapServers(), "test-relay-verifier", "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
    }
}
