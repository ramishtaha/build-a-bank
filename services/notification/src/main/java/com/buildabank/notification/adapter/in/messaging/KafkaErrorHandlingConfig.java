// services/notification/src/main/java/com/buildabank/notification/adapter/in/messaging/KafkaErrorHandlingConfig.java
package com.buildabank.notification.adapter.in.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Step 21/26 · retries + Dead-Letter Topic for the inbound messaging adapter. When the listener throws on a
 * poison message, the {@link DefaultErrorHandler} retries a few times then the {@link DeadLetterPublishingRecoverer}
 * republishes it to {@code <topic>.DLT} instead of blocking the partition. Part of the messaging adapter
 * (transport concern), not the core. Boot wires the single {@code CommonErrorHandler} bean into the listener factory.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 2L));   // 2 retries then → DLT
    }
}
