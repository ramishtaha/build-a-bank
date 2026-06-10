// services/notification/src/main/java/com/buildabank/notification/TransferEventParser.java
package com.buildabank.notification;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Step 25 · the parsing concern, extracted from the consumer (SRP). Turns the JSON wire payload into a
 * {@link TransferEvent}, isolating the Jackson coupling here. An un-parseable ("poison") payload <strong>throws
 * on purpose</strong> — the consumer lets it propagate so the container routes it to the Dead-Letter Topic
 * (Step 21), rather than swallowing it.
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
