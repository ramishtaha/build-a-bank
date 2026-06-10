// services/demand-account/src/test/java/com/buildabank/account/PaymentExactlyOnceCapstoneTest.java
package com.buildabank.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.redpanda.RedpandaContainer;

import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.IdempotencyRecordRepository;
import com.buildabank.account.domain.LedgerEntryRepository;
import com.buildabank.account.outbox.OutboxEventRepository;
import com.buildabank.account.outbox.OutboxRelay;
import com.buildabank.account.service.IdempotentTransferService;
import com.buildabank.account.service.TransferService;

/**
 * 🎓 <strong>Phase-D capstone</strong> — trace a payment end-to-end and prove <strong>exactly-once effect</strong>
 * under a forced retry, tying together the whole phase on real infrastructure (Postgres + Redpanda):
 * <ol>
 *   <li><strong>Idempotency-Key</strong> (Step 14/21): the same key replays to the same transactionId — money
 *       moves once.</li>
 *   <li><strong>Outbox</strong> (Step 20): the transfer atomically wrote an event row; the relay publishes it
 *       to Kafka.</li>
 *   <li><strong>At-least-once + idempotent consumer</strong> (Step 19/20): we FORCE a duplicate delivery (the
 *       gap the relay's publish-then-mark allows), and an id-deduping consumer applies it <strong>once</strong>.</li>
 * </ol>
 */
@SpringBootTest
@Import({ContainersConfig.class, RedpandaContainers.class})
class PaymentExactlyOnceCapstoneTest {

    private static final String TOPIC = "transfers.completed";

    @Autowired
    TransferService transfers;

    @Autowired
    IdempotentTransferService idempotentTransfers;

    @Autowired
    OutboxRelay relay;

    @Autowired
    OutboxEventRepository outbox;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @Autowired
    IdempotencyRecordRepository idempotencyKeys;

    @Autowired
    RedpandaContainer redpanda;

    @BeforeEach
    void clean() {
        idempotencyKeys.deleteAll();
        outbox.deleteAll();
        ledger.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void paymentEndToEnd_idempotentTransfer_outboxToKafka_forcedRedelivery_appliesExactlyOnce() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("100.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));

        // 1) Idempotency-Key: a retried payment with the same key moves money ONCE.
        UUID txId = idempotentTransfers.transfer("CAP-KEY", "ACC-A", "ACC-B", new BigDecimal("40.00"), "capstone");
        UUID retry = idempotentTransfers.transfer("CAP-KEY", "ACC-A", "ACC-B", new BigDecimal("40.00"), "capstone");
        assertThat(retry).isEqualTo(txId);
        assertThat(accounts.findByAccountNumber("ACC-A").orElseThrow().getBalance()).isEqualByComparingTo("60.00");

        // 2) Outbox → Kafka: the transfer atomically wrote one outbox row; the relay publishes it.
        assertThat(outbox.countByPublishedFalse()).isEqualTo(1);
        assertThat(relay.publishPending()).isEqualTo(1);

        // 3) At-least-once + idempotent consumer = exactly-once EFFECT: consume the published event, then FORCE
        //    a duplicate redelivery (same key + payload), consume again, and dedupe by eventId. We scope to
        //    THIS payment's records (value contains our txId) — the shared topic may carry other tests' events.
        Set<String> appliedEventIds = new HashSet<>();
        int deliveriesOfMyPayment = 0;
        try (Consumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(TOPIC));

            ConsumerRecord<String, String> published = null;
            for (ConsumerRecord<String, String> r : KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15))) {
                if (r.value().contains(txId.toString())) {
                    published = r;
                    deliveriesOfMyPayment++;
                    appliedEventIds.add(r.key());
                }
            }
            assertThat(published).as("the relay published our event").isNotNull();

            // forced redelivery — the same record sent again (an at-least-once duplicate)
            try (Producer<String, String> producer = testProducer()) {
                producer.send(new ProducerRecord<>(TOPIC, published.key(), published.value()));
                producer.flush();
            }
            for (ConsumerRecord<String, String> r : KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15))) {
                if (r.value().contains(txId.toString())) {
                    deliveriesOfMyPayment++;
                    appliedEventIds.add(r.key());
                }
            }
        }

        // Our payment's event was DELIVERED at least twice (original + the forced duplicate)...
        assertThat(deliveriesOfMyPayment).isGreaterThanOrEqualTo(2);
        // ...but dedupe by eventId means it would be APPLIED exactly once — exactly-once effect.
        assertThat(appliedEventIds).hasSize(1);
    }

    private Consumer<String, String> testConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                redpanda.getBootstrapServers(), "capstone-verifier", "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
    }

    private Producer<String, String> testProducer() {
        return new KafkaProducer<>(KafkaTestUtils.producerProps(redpanda.getBootstrapServers()),
                new StringSerializer(), new StringSerializer());
    }
}
