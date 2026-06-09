// services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookSigner.java
package com.buildabank.account.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Signs and verifies outbound webhooks with <strong>HMAC-SHA256</strong>. The signature is computed over
 * {@code "<timestamp>.<body>"} with a shared secret, so a receiver can prove (a) the payload wasn't tampered
 * with and (b) it really came from us. Including the timestamp in the signed material — and rejecting old
 * timestamps on verify — gives <strong>replay protection</strong>: an attacker can't re-send a captured,
 * still-valid request hours later.
 *
 * <p>This is the same scheme Stripe/GitHub-style webhooks use. We compare signatures in
 * <strong>constant time</strong> to avoid leaking, via timing, how much of a guessed signature was correct.
 */
@Component
public class WebhookSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /** Hex HMAC-SHA256 of {@code timestamp + "." + body} keyed by {@code secret}. */
    public String sign(String secret, long timestampEpochSeconds, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal((timestampEpochSeconds + "." + body).getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign webhook", e);
        }
    }

    /**
     * Verify a received signature: recompute and compare in constant time, AND reject timestamps outside the
     * tolerance window (replay protection). {@code nowEpochSeconds} is passed in so it's testable.
     */
    public boolean verify(String secret, long timestampEpochSeconds, String body, String providedSignature,
                          long nowEpochSeconds, long toleranceSeconds) {
        if (Math.abs(nowEpochSeconds - timestampEpochSeconds) > toleranceSeconds) {
            return false;   // too old (or too far in the future) → likely a replay
        }
        String expected = sign(secret, timestampEpochSeconds, body);
        return constantTimeEquals(expected, providedSignature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
