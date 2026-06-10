// services/notification/src/test/java/com/buildabank/notification/DeadLetterTest.java
package com.buildabank.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.redpanda.RedpandaContainer;

import com.buildabank.notification.adapter.in.messaging.TransferEventConsumer;

/**
 * Step 21 · proves <strong>retries + the Dead-Letter Topic</strong> on a REAL broker (Redpanda). A poison
 * (un-parseable) message is retried and then routed to {@code transfers.completed.DLT} instead of blocking
 * the partition; a valid message sent alongside it is still processed normally.
 */
@SpringBootTest
@Import(RedpandaContainers.class)
class DeadLetterTest {

    private static final String TOPIC = "transfers.completed";
    private static final String DLT = "transfers.completed.DLT";
    private static final String POISON = "<<<this-is-not-valid-json>>>";

    @Autowired
    TransferEventConsumer consumer;

    @Autowired
    RedpandaContainer redpanda;

    @Test
    void poisonMessageGoesToTheDeadLetterTopic_whileGoodMessagesStillProcess() {
        String goodEventId = UUID.randomUUID().toString();

        try (Producer<String, String> producer = testProducer()) {
            producer.send(new ProducerRecord<>(TOPIC, "poison", POISON));                       // un-parseable
            producer.send(new ProducerRecord<>(TOPIC, goodEventId, goodPayload(goodEventId)));  // valid
            producer.flush();
        }

        // The valid message is processed despite the poison one ahead of it.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(consumer.appliedCount()).isGreaterThanOrEqualTo(1));

        // The poison message ends up on the DLT (after the retries are exhausted), value preserved.
        try (Consumer<String, String> dltConsumer = testConsumer()) {
            dltConsumer.subscribe(List.of(DLT));
            ConsumerRecords<String, String> dltRecords =
                    KafkaTestUtils.getRecords(dltConsumer, Duration.ofSeconds(20));
            assertThat(dltRecords.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, String> dead = dltRecords.iterator().next();
            assertThat(dead.value()).isEqualTo(POISON);
        }
    }

    private Producer<String, String> testProducer() {
        return new KafkaProducer<>(KafkaTestUtils.producerProps(redpanda.getBootstrapServers()),
                new StringSerializer(), new StringSerializer());
    }

    private Consumer<String, String> testConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                redpanda.getBootstrapServers(), "dlt-verifier", "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
    }

    private static String goodPayload(String eventId) {
        return """
                {"eventId":"%s","transactionId":"%s","from":"ACC-A","to":"ACC-B","amount":40.00,"occurredAt":"2026-06-10T00:00:00Z"}
                """.formatted(eventId, UUID.randomUUID());
    }
}
