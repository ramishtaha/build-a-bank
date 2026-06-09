// services/notification/src/main/java/com/buildabank/notification/KafkaErrorHandlingConfig.java
package com.buildabank.notification;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Step 21 · <strong>retries + Dead-Letter Topic (DLQ)</strong> for the Kafka consumer. When
 * {@link TransferEventConsumer} throws on a message (e.g. an un-parseable "poison" payload), Spring Kafka's
 * {@link DefaultErrorHandler} retries it a few times; if it still fails, the {@link DeadLetterPublishingRecoverer}
 * republishes the original record to a dead-letter topic ({@code <topic>.DLT}) instead of blocking the
 * partition forever. The poison message is quarantined for inspection while good messages keep flowing.
 *
 * <p>Boot auto-wires a single {@code CommonErrorHandler} bean into the listener container factory, so just
 * declaring this bean activates it. The recoverer uses the auto-configured {@link KafkaTemplate} (String
 * serializers — see application.yml) to publish the failed record verbatim.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        // 2 retries (no delay) then send to the DLT — fast and deterministic for a poison message.
        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 2L));
    }
}
