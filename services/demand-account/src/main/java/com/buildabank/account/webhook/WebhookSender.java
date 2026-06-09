// services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookSender.java
package com.buildabank.account.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delivers a signed webhook over HTTP with <strong>bounded retries</strong>. Webhook delivery is
 * <em>at-least-once</em>: networks fail, receivers hiccup, so we retry a few times with backoff — which is
 * exactly why receivers must be <strong>idempotent</strong> (they may see the same event twice). Each attempt
 * carries the HMAC signature and timestamp ({@link WebhookSigner}) so the receiver can verify authenticity
 * and reject replays.
 *
 * <p>(This sends directly for teaching clarity. In production the <em>dual-write problem</em> — the DB
 * transaction commits but the webhook send fails, or vice-versa — is solved by the <strong>Outbox
 * pattern</strong> in Step 20; we flag that explicitly rather than pretend this is complete.)
 */
@Component
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);
    private static final int MAX_ATTEMPTS = 3;

    private final WebhookSigner signer;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public WebhookSender(WebhookSigner signer) {
        this.signer = signer;
    }

    /** POST the signed body to {@code url}; retry up to 3 times on failure. Returns true if a 2xx was received. */
    public boolean send(String url, String secret, String body) {
        long timestamp = Instant.now().getEpochSecond();
        String signature = signer.sign(secret, timestamp, body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Timestamp", Long.toString(timestamp))
                .header("X-Webhook-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() / 100 == 2) {
                    log.info("webhook delivered to {} on attempt {} ({})", url, attempt, response.statusCode());
                    return true;
                }
                log.warn("webhook to {} got {} on attempt {}", url, response.statusCode(), attempt);
            } catch (Exception e) {
                log.warn("webhook to {} failed on attempt {}: {}", url, attempt, e.toString());
            }
            sleepBackoff(attempt);
        }
        log.error("webhook to {} FAILED after {} attempts", url, MAX_ATTEMPTS);
        return false;
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(50L * attempt);   // simple linear backoff (small, so tests stay fast)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
