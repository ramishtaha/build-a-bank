// services/demand-account/src/test/java/com/buildabank/account/webhook/WebhookDeliveryTest.java
package com.buildabank.account.webhook;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

/**
 * End-to-end webhook delivery over a real (in-test) HTTP receiver — no Spring, no Docker. Proves the
 * {@link WebhookSender} delivers a signed payload that the receiver can <strong>verify</strong> with the
 * shared secret, and that delivery <strong>retries</strong> on a transient failure (at-least-once).
 */
class WebhookDeliveryTest {

    private static final String SECRET = "whsec_delivery_test";
    private final WebhookSigner signer = new WebhookSigner();
    private final WebhookSender sender = new WebhookSender(signer);

    @Test
    void deliversASignedWebhookTheReceiverCanVerify() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicBoolean signatureValid = new AtomicBoolean(false);
        CountDownLatch received = new CountDownLatch(1);

        server.createContext("/webhooks", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            long timestamp = Long.parseLong(exchange.getRequestHeaders().getFirst("X-Webhook-Timestamp"));
            String signature = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
            // The receiver verifies authenticity + freshness exactly as a partner would.
            boolean ok = signer.verify(SECRET, timestamp, body, signature, Instant.now().getEpochSecond(), 300);
            signatureValid.set(ok);
            exchange.sendResponseHeaders(ok ? 200 : 400, -1);
            exchange.close();
            received.countDown();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/webhooks";
            boolean delivered = sender.send(url, SECRET, "{\"event\":\"transfer.completed\",\"amount\":50.00}");

            assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(delivered).isTrue();
            assertThat(signatureValid).isTrue();   // the receiver validated our HMAC signature + timestamp
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retriesOnTransientFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicInteger calls = new AtomicInteger();

        server.createContext("/webhooks", exchange -> {
            int attempt = calls.incrementAndGet();
            exchange.sendResponseHeaders(attempt == 1 ? 500 : 200, -1);   // fail first, then accept
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/webhooks";
            boolean delivered = sender.send(url, SECRET, "{\"event\":\"x\"}");

            assertThat(delivered).isTrue();
            assertThat(calls.get()).isGreaterThanOrEqualTo(2);   // it retried after the 500
        } finally {
            server.stop(0);
        }
    }
}
