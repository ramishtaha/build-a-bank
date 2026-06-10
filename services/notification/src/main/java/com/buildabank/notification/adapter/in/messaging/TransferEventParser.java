// services/notification/src/main/java/com/buildabank/notification/adapter/in/messaging/TransferEventParser.java
package com.buildabank.notification.adapter.in.messaging;

import org.springframework.stereotype.Component;

import com.buildabank.notification.domain.TransferEvent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Step 26 (hexagonal) · part of the inbound MESSAGING adapter: translates the Kafka JSON wire payload into a
 * domain {@link TransferEvent}. The Jackson/transport coupling lives out here in the adapter ring, never in the
 * domain or application core. A poison payload <strong>throws</strong> so the listener routes it to the
 * Dead-Letter Topic (Step 21).
 */
@Component
public class TransferEventParser {

    private final ObjectMapper objectMapper;   // Boot 4 default is Jackson 3 (tools.jackson)

    public TransferEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TransferEvent parse(String payload) {
        JsonNode node = objectMapper.readTree(payload);   // throws on malformed JSON → propagates → DLT
        return new TransferEvent(
                node.get("eventId").asText(),
                node.get("transactionId").asText(),
                node.get("from").asText(),
                node.get("to").asText(),
                node.get("amount").decimalValue(),
                node.get("occurredAt").asText());
    }
}
