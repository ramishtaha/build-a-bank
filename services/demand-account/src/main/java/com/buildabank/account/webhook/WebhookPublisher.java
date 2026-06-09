// services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookPublisher.java
package com.buildabank.account.webhook;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds the {@code transfer.completed} event JSON and hands it to the {@link WebhookSender}. Gated by
 * config: if {@code bank.webhook.url} is unset (the default), publishing is a no-op — so local runs and
 * tests that don't care about webhooks aren't affected. The secret comes from config too (never hard-coded
 * in real life — Vault/secrets in Phase H).
 */
@Component
public class WebhookPublisher {

    private final WebhookSender sender;
    // Own a Jackson mapper rather than inject one: Spring Boot 4 defaults the web stack to Jackson 3, so a
    // Jackson-2 com.fasterxml ObjectMapper bean isn't auto-created. A self-owned mapper keeps this independent.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String url;
    private final String secret;

    public WebhookPublisher(WebhookSender sender,
                            @Value("${bank.webhook.url:}") String url,
                            @Value("${bank.webhook.secret:demo-secret}") String secret) {
        this.sender = sender;
        this.url = url;
        this.secret = secret;
    }

    /** Emit a signed {@code transfer.completed} webhook (no-op if no URL is configured). */
    public void transferCompleted(UUID transactionId, String from, String to, BigDecimal amount) {
        if (url == null || url.isBlank()) {
            return;   // webhooks not configured → skip
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "transfer.completed");
        event.put("transactionId", transactionId.toString());
        event.put("from", from);
        event.put("to", to);
        event.put("amount", amount);
        try {
            sender.send(url, secret, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException("failed to publish webhook", e);
        }
    }
}
