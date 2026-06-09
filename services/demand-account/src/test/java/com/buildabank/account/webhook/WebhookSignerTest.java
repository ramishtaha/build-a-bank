// services/demand-account/src/test/java/com/buildabank/account/webhook/WebhookSignerTest.java
package com.buildabank.account.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure unit tests for the HMAC webhook signing — authenticity, tamper-detection, and replay protection. */
class WebhookSignerTest {

    private final WebhookSigner signer = new WebhookSigner();
    private static final String SECRET = "whsec_test_0123456789";
    private static final long TS = 1_700_000_000L;
    private static final String BODY = "{\"event\":\"transfer.completed\",\"amount\":50.00}";

    @Test
    void aFreshValidSignatureVerifies() {
        String signature = signer.sign(SECRET, TS, BODY);
        // verify "now" is the same second → within tolerance
        assertThat(signer.verify(SECRET, TS, BODY, signature, TS, 300)).isTrue();
    }

    @Test
    void aTamperedBodyIsRejected() {
        String signature = signer.sign(SECRET, TS, BODY);
        String tampered = BODY.replace("50.00", "5000.00");   // attacker bumps the amount
        assertThat(signer.verify(SECRET, TS, tampered, signature, TS, 300)).isFalse();
    }

    @Test
    void theWrongSecretIsRejected() {
        String signature = signer.sign(SECRET, TS, BODY);
        assertThat(signer.verify("whsec_attacker", TS, BODY, signature, TS, 300)).isFalse();
    }

    @Test
    void aStaleTimestampIsRejected_replayProtection() {
        String signature = signer.sign(SECRET, TS, BODY);
        long muchLater = TS + 3_600;   // one hour later, tolerance is 300s
        assertThat(signer.verify(SECRET, TS, BODY, signature, muchLater, 300)).isFalse();
    }
}
